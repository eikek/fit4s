package fit4s

import fit4s.FitMessage.DefinitionMessage
import fit4s.profile.messages.Msg
import fit4s.profile.messages.Msg.{ArrayDef, FieldWithCodec}
import fit4s.profile.types.{
  ArrayFieldType,
  BaseTypeCodec,
  DateTime,
  FitBaseType,
  GenFieldType,
  LongFieldType,
  StringFieldType
}
import scodec.{Attempt, DecodeResult, Decoder, Err}
import scodec.bits.BitVector
import scodec.codecs._

private object DataDecoder {

  final case class DataDecodeResult(fields: List[FieldDecodeResult]) {
    override def toString = {
      val fieldToString =
        fields
          .map {
            case r: FieldDecodeResult.Success =>
              s"${r.globalField.fieldName}=${r.valueString}"
            case r: FieldDecodeResult.LocalSuccess =>
              s"${r.localField.fieldDefNum}=${r.value}"
            case r: FieldDecodeResult.DecodeError =>
              s"Error: ${r.err.messageWithContext}"
            case r: FieldDecodeResult.NoReferenceSubfield =>
              s"No subfield reference: ${r.globalField.fieldName}"
            case r: FieldDecodeResult.InvalidValue =>
              s"Invalid value '0x${BaseTypeCodec
                  .invalidValue(r.localField.baseType.fitBaseType)
                  .toHex}' for field ${r.localField.fieldDefNum}/${r.localField.baseType.fitBaseType}"
          }
          .mkString(", ")

      s"DataDecodeResult($fieldToString)"
    }
  }

  sealed trait FieldDecodeResult {
    def widen: FieldDecodeResult = this

    def messageField: Option[Msg.FieldWithCodec[GenFieldType]]
    def successValue: Option[GenFieldType]
  }
  object FieldDecodeResult {
    final case class InvalidValue(localField: FieldDefinition) extends FieldDecodeResult {
      val messageField = None
      val successValue = None
    }

    final case class LocalSuccess(
        localField: FieldDefinition,
        value: GenFieldType
    ) extends FieldDecodeResult {
      val messageField = None
      val successValue = Some(value)
    }

    final case class Success(
        localField: FieldDefinition,
        globalField: Msg.FieldWithCodec[GenFieldType],
        value: GenFieldType
    ) extends FieldDecodeResult {
      val successValue = Some(value)
      val messageField = Some(globalField)

      def scaledValue: Option[List[Double]] =
        (value, globalField.scale) match {
          case (LongFieldType(rv, _), List(scale)) =>
            Some(List(rv / scale))

          case (ArrayFieldType.LongArray(list), List(scale)) =>
            Some(list.map(_ / scale))

          case _ => None
        }

      def valueString: String = {
        val amount = scaledValue
          .map {
            case h :: Nil => h.toString
            case l        => l.toString
          }
          .getOrElse(value match {
            case LongFieldType(rv, _) => rv.toString
            case dt: DateTime         => dt.asInstant.toString
            case _                    => value.toString
          })
        val unit = globalField.unit.map(_.name).getOrElse("")
        s"$amount$unit"
      }
    }

    final case class DecodeError(
        err: Err
    ) extends FieldDecodeResult {
      val successValue = None
      val messageField = None
    }

    final case class NoReferenceSubfield(
        localField: FieldDefinition,
        globalField: Msg.Field[GenFieldType]
    ) extends FieldDecodeResult {
      val successValue = None
      val messageField = Some(globalField)
    }
  }

  def apply(definition: DefinitionMessage): Decoder[DataDecodeResult] =
    definition.profileMsg.map(pm => create(definition, pm)).getOrElse(create(definition))

  /** Decodes a single data message according to the given definition message and matching
    * profile message.
    */
  def create(dm: DefinitionMessage, pm: Msg): Decoder[DataDecodeResult] = {
    def fieldDecoder(previous: List[FieldDecodeResult], localField: FieldDefinition) =
      pm.findField(localField.fieldDefNum) match {
        case Some(globalField) =>
          decodeKnownField(previous, dm, localField, globalField)

        case None =>
          decodeUnknownField(dm, localField)
      }

    (bits: BitVector) => {
      @annotation.tailrec
      def go(
          fields: List[FieldDefinition],
          input: BitVector,
          results: List[FieldDecodeResult]
      ): Attempt[DecodeResult[List[FieldDecodeResult]]] =
        fields match {
          case Nil => Attempt.successful(DecodeResult(results, input))
          case field :: m =>
            fieldDecoder(results, field).decode(input) match {
              case Attempt.Successful(result) =>
                go(m, result.remainder, result.value :: results)
              case Attempt.Failure(err) =>
                val r = FieldDecodeResult.DecodeError(err)
                go(m, input.drop(field.sizeBytes * 8), r :: results)
            }
        }

      go(dm.fields, bits, Nil).map(_.map(DataDecodeResult.apply))
    }
  }

  /** Decodes a data message according to the definition message where no global fit
    * message exists.
    */
  def create(dm: DefinitionMessage): Decoder[DataDecodeResult] = { (bits: BitVector) =>
    @annotation.tailrec
    def go(
        fields: List[FieldDefinition],
        input: BitVector,
        results: List[FieldDecodeResult]
    ): Attempt[DecodeResult[List[FieldDecodeResult]]] =
      fields match {
        case Nil => Attempt.successful(DecodeResult(results, input))
        case field :: m =>
          decodeUnknownField(dm, field).decode(input) match {
            case Attempt.Successful(result) =>
              go(m, result.remainder, result.value :: results)
            case Attempt.Failure(err) =>
              val r = FieldDecodeResult.DecodeError(err)
              go(m, input.drop(field.sizeBytes * 8), r :: results)
          }
      }

    go(dm.fields, bits, Nil).map(_.map(DataDecodeResult.apply))
  }

  def withInvalidValue(
      localField: FieldDefinition
  )(ifValid: Decoder[FieldDecodeResult]): Decoder[FieldDecodeResult] =
    peek(bytes(BaseTypeCodec.length(localField.baseType.fitBaseType))).flatMap { bv =>
      if (bv == BaseTypeCodec.invalidValue(localField.baseType.fitBaseType)) {
        bytes(localField.sizeBytes).asDecoder.map(_ =>
          FieldDecodeResult.InvalidValue(localField)
        )
      } else {
        ifValid
      }
    }

  private def localResult(
      localField: FieldDefinition
  )(d: Decoder[GenFieldType]): Decoder[FieldDecodeResult] =
    Decoder(bits =>
      d.decode(bits) match {
        case Attempt.Successful(result) =>
          Attempt.successful(
            DecodeResult(
              FieldDecodeResult.LocalSuccess(localField, result.value),
              result.remainder
            )
          )
        case f @ Attempt.Failure(_) => f
      }
    )

  def decodeUnknownField(
      dm: DefinitionMessage,
      localField: FieldDefinition
  ): Decoder[FieldDecodeResult] =
    withInvalidValue(localField) {
      localField.baseType.fitBaseType match {
        case FitBaseType.String =>
          localResult(localField)(StringFieldType.codec(localField.sizeBytes))

        case ft =>
          val baseLen = BaseTypeCodec.length(ft)
          if (localField.sizeBytes > baseLen) {
            if (localField.sizeBytes % baseLen != 0) {
              bytes(localField.sizeBytes).asDecoder.map(_ =>
                FieldDecodeResult.DecodeError(
                  Err(
                    s"Field '$localField' size is not a multiple of it's base type length '$baseLen'!"
                  )
                )
              )
            } else
              localResult(localField)(
                ArrayFieldType.codecLong(localField.sizeBytes, dm.archType, ft)
              )
          } else {
            localResult(localField)(LongFieldType.codec(dm.archType, ft))
          }
      }

    }

  def decodeKnownField(
      previous: List[FieldDecodeResult],
      dm: DefinitionMessage,
      localField: FieldDefinition,
      globalField: Msg.Field[GenFieldType]
  ): Decoder[FieldDecodeResult] =
    withInvalidValue(localField) {
      if (globalField.isDynamicField) {
        // look in previous decoded values, if the referenced field matches
        // TODO improve data structures to better support searches
        // TODO support for components
        val subFieldMatch =
          globalField.subFields.find { subField =>
            previous.collectFirst { case p: FieldDecodeResult.Success =>
              subField.references.exists(ref =>
                ref.refField == p.globalField && ref.refFieldValue == p.value
              )
            }.isDefined
          }

        subFieldMatch match {
          case Some(subField) =>
            decodeFieldWithCodec(
              dm,
              localField,
              subField.asInstanceOf[Msg.SubField[GenFieldType]]
            )

          case None =>
            Decoder.point(FieldDecodeResult.NoReferenceSubfield(localField, globalField))
        }
      } else {
        decodeFieldWithCodec(dm, localField, globalField)
      }
    }

  private def decodeFieldWithCodec(
      dm: DefinitionMessage,
      localField: FieldDefinition,
      field: FieldWithCodec[GenFieldType]
  ): Decoder[FieldDecodeResult] = {
    val fc = field
      .fieldCodec(localField)(dm.archType)
      .asDecoder
      .map[FieldDecodeResult.Success](value =>
        FieldDecodeResult.Success(localField, field, value)
      )

    val ac = fixedSizeBytes(
      localField.sizeBytes,
      list(field.fieldCodec(localField)(dm.archType))
    ).asDecoder
      .map(ArrayFieldType.apply)
      .map(v => FieldDecodeResult.Success(localField, field, v))

    field.isArray match {
      case ArrayDef.NoArray =>
        fc.asDecoder

      case ArrayDef.DynamicSize =>
        if (localField.sizeBytes > field.baseTypeLen) ac else fc

      case ArrayDef.Sized(n) =>
        if (n > 1) ac else fc
    }
  }
}
