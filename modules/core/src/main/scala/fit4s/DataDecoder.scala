package fit4s

import fit4s.FitMessage.DefinitionMessage
import fit4s.data.Nel
import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.messages.Msg.{ArrayDef, FieldWithCodec}
import fit4s.profile.types.{
  ArrayFieldType,
  BaseTypeCodec,
  FitBaseType,
  TypedValue,
  LongFieldType,
  StringFieldType
}
import scodec.{Attempt, DecodeResult, Decoder, Err}
import scodec.bits.BitVector
import scodec.codecs._

// TODO make private
object DataDecoder {

  final case class DataDecodeResult(fields: List[FieldDecodeResult]) {
    def findField[A <: TypedValue](ft: Msg.FieldWithCodec[A]): Option[FieldValue[A]] =
      fields.collectFirst {
        case r: FieldDecodeResult.Success if r.fieldValue.field == ft =>
          r.fieldValue.asInstanceOf[FieldValue[A]]
      }

    override def toString = {
      val fieldToString =
        fields
          .map {
            case r: FieldDecodeResult.Success =>
              s"${r.fieldValue}"
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
    def isKnownSuccess: Boolean
  }
  object FieldDecodeResult {
    final case class InvalidValue(localField: FieldDefinition) extends FieldDecodeResult {
      val isKnownSuccess = false
    }

    final case class LocalSuccess(
        localField: FieldDefinition,
        value: TypedValue
    ) extends FieldDecodeResult {
      val isKnownSuccess = false
    }

    final case class Success(
        localField: FieldDefinition,
        fieldValue: FieldValue[TypedValue]
    ) extends FieldDecodeResult {
      val isKnownSuccess = true
    }

    final case class DecodeError(
        localField: FieldDefinition,
        err: Err
    ) extends FieldDecodeResult {
      val isKnownSuccess = false
    }

    final case class NoReferenceSubfield(
        localField: FieldDefinition,
        globalField: Msg.Field[TypedValue]
    ) extends FieldDecodeResult {
      val isKnownSuccess = false
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
                val r = FieldDecodeResult.DecodeError(field, err)
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
              val r = FieldDecodeResult.DecodeError(field, err)
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
  )(d: Decoder[TypedValue]): Decoder[FieldDecodeResult] =
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
                  localField,
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
      globalField: Msg.Field[TypedValue]
  ): Decoder[FieldDecodeResult] =
    withInvalidValue(localField) {
      if (globalField.isDynamicField) {
        // look in previous decoded values, if the referenced field matches
        // TODO improve data structures to better support searches
        // TODO support for components
        val subFieldMatch =
          globalField.subFields.find { subField =>
            previous.collectFirst { case p: FieldDecodeResult.Success =>
              subField.references.exists(ref => ref.asFieldValue == p.fieldValue)
            }.isDefined
          }

        subFieldMatch match {
          case Some(subField) =>
            decodeFieldWithCodec(
              dm,
              localField,
              subField.asInstanceOf[Msg.SubField[TypedValue]]
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
      field: FieldWithCodec[TypedValue]
  ): Decoder[FieldDecodeResult] = {
    val fc = field
      .fieldCodec(localField)(dm.archType)
      .asDecoder
      .map[FieldDecodeResult.Success](value =>
        FieldDecodeResult.Success(localField, FieldValue(field, value))
      )

    val ac = fixedSizeBytes(
      localField.sizeBytes,
      list(field.fieldCodec(localField)(dm.archType))
    ).asDecoder
      .map(Nel.unsafeFromList)
      .map(ArrayFieldType.apply)
      .map(v => FieldDecodeResult.Success(localField, FieldValue(field, v)))

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
