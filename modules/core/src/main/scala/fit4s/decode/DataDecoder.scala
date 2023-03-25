package fit4s.decode

import fit4s.FitMessage.DefinitionMessage
import fit4s.data.Nel
import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.messages.Msg.{ArrayDef, FieldWithCodec}
import fit4s.profile.types.{ArrayFieldType, BaseTypeCodec, BaseTypedValue, TypedValue}
import fit4s.{DataDecodeResult, FieldDecodeResult, FieldDefinition}
import scodec.bits.BitVector
import scodec.codecs.{bytes, fixedSizeBytes, list, peek}
import scodec.{Attempt, DecodeResult, Decoder, Err}

private[fit4s] object DataDecoder {

  def apply(definition: DefinitionMessage): Decoder[DataDecodeResult] =
    definition.profileMsg.map(pm => create(definition, pm)).getOrElse(create(definition))

  /** Decodes a single data message according to the given definition message and matching
    * profile message.
    */
  def create(dm: DefinitionMessage, pm: Msg): Decoder[DataDecodeResult] = {
    // must expand all fields with components into its generated fields
    // going recursively until no components exist. then continue with this decoding
    // perhaps it is good to capture single fields + its byte-vector first, something
    // like (localField, profileField, ByteVector) and then flatMap components
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
  private def create(dm: DefinitionMessage): Decoder[DataDecodeResult] = {
    (bits: BitVector) =>
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
  )(d: Decoder[TypedValue[_]]): Decoder[FieldDecodeResult] =
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
      val base = localField.baseType.fitBaseType
      val baseLen = BaseTypeCodec.length(base)
      if (localField.sizeBytes > baseLen && localField.sizeBytes % baseLen != 0) {
        bytes(localField.sizeBytes).asDecoder.map(_ =>
          FieldDecodeResult.DecodeError(
            localField,
            Err(
              s"Field '$localField' size is not a multiple of it's base type length '$baseLen'!"
            )
          )
        )
      } else {
        if (localField.sizeBytes > baseLen) {
          localResult(localField)(
            ArrayFieldType.codec(
              localField.sizeBytes,
              dm.archType,
              localField.baseType.fitBaseType
            )
          )
        } else {
          localResult(localField)(BaseTypedValue.codec(base, dm.archType))
        }
      }
    }

  def decodeKnownField(
      previous: List[FieldDecodeResult],
      dm: DefinitionMessage,
      localField: FieldDefinition,
      globalField: Msg.Field[TypedValue[_]]
  ): Decoder[FieldDecodeResult] =
    withInvalidValue(localField) {
      if (globalField.isDynamicField) {
        // look in previous decoded values, if the referenced field matches
        // TODO improve data structures to better support searches
        // TODO support for components
        val activeSubField =
          globalField.subFields.find { subField =>
            previous.collectFirst { case p: FieldDecodeResult.Success =>
              subField.references.exists(ref => ref.asFieldValue == p.fieldValue)
            }.isDefined
          }

        activeSubField match {
          case Some(subField) =>
            decodeFieldWithCodec(
              dm,
              localField,
              subField.asInstanceOf[Msg.SubField[TypedValue[_]]]
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
      field: FieldWithCodec[TypedValue[_]]
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
      .map(v => ArrayFieldType(v, localField.baseType.fitBaseType))
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
