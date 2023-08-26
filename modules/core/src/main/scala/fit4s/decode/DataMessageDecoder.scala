package fit4s.decode

import scala.annotation.tailrec

import fit4s.FitMessage.{DataMessage, DefinitionMessage}
import fit4s.decode.CodecUtils._
import fit4s.decode.DataField.KnownField
import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.messages.Msg.{ArrayDef, FieldWithCodec}
import fit4s.profile.types._
import fit4s.util.Nel
import fit4s.{FieldDecodeResult, FieldDefinition}

import scodec._
import scodec.bits.{BitVector, ByteOrdering, ByteVector}
import scodec.codecs._

object DataMessageDecoder {
  def create(definition: DefinitionMessage): Decoder[DataFields] =
    Decoder.apply { bits =>
      val dm = DataMessage(definition, bits.bytes)
      Attempt.successful(DecodeResult(decode(dm), BitVector.empty))
    }

  def decode(dm: DataMessage): DataFields = {
    val init = makeDataFields(dm)
    dm.definition.profileMsg match {
      case Some(pm) => init.flatMap(expandField(pm, init))
      case None     => init
    }
  }

  def makeDataFields(dm: DataMessage): DataFields = {
    @tailrec
    def loop(
        fields: List[FieldDefinition],
        bytes: ByteVector,
        result: Vector[DataField]
    ): Vector[DataField] =
      fields match {
        case Nil => result.reverse
        case h :: t =>
          val field = dm.definition.profileMsg.flatMap(_.findField(h.fieldDefNum))
          val (raw, next) = bytes.splitAt(h.sizeBytes)
          loop(t, next, DataField(h, dm.definition.archType, field, raw) +: result)
      }

    DataFields(loop(dm.definition.fields, dm.raw, Vector.empty))
  }

  /** Fields are expanded as follows:
    *
    * ## Dynamic fields
    *
    * A dynamic field interpretation depends on the value of another field in the same
    * message. The field defines references to other fields in its sub-fields. Each
    * reference consists of a field name and a value, where the field name links to
    * another field in the message. If this other field has the value in the reference,
    * then it is considered "active". That is, the enclosing field is interpreted using
    * the sub-fields metadata.
    *
    * ## Components
    *
    * Components define a list of field names and a list of bits with the same size. Each
    * of the fields in the components list is defined via its corresponding bit size. IOW
    * a single fields contains multiple fields. The field names in the components list are
    * top-level fields which can have again subfields.
    *
    * Sub-fields and top-level fields may have components.
    *
    * Top-level fields may have either sub-fields or components.
    *
    * The result contains the given field and potentially its expanded fields.
    */
  def expandField(profileMsg: Msg, allFields: DataFields)(field: DataField): DataFields =
    field match {
      case DynamicField(subFields) =>
        val activeSubField =
          subFields.toList.find { subField =>
            subField.references.exists { ref =>
              assert(
                ref.refField != null,
                s"ref '$ref' of subfield '$subField' is null?!"
              )
              val x = allFields.getByName(ref.refField.fieldName)
              x
                .exists(
                  _.decodedValue.fold(
                    _ => false,
                    r => r.asSuccess.map(_.fieldValue.value).contains(ref.refFieldValue)
                  )
                )
            }
          }

        activeSubField
          .map { sf =>
            val sub = DataField.KnownField(field.local, field.byteOrdering, sf, field.raw)
            if (ComponentField.test(sub))
              field +: expandField(profileMsg, allFields)(sub)
            else
              DataFields.of(field, sub)
          }
          .getOrElse(DataFields.of(field))

      case ComponentField(components, f) =>
        @tailrec
        def loop(
            bits: BitVector,
            comp: List[(String, Int)],
            result: Vector[DataField]
        ): Vector[DataField] =
          comp match {
            case Nil => result
            case (name, len) :: t =>
              profileMsg.getFieldByName(name) match {
                case Some(cfield) =>
                  val nextData = bits.takeRight(len)
                  if (nextData.sizeLessThan(len)) {
                    result
                  } else {
                    val cfieldMinLen = cfield.baseTypeLen * 8
                    val nextBits = bits.takeRight(len) match {
                      case b if len < cfieldMinLen =>
                        b.lowPaddedByteVectorToLength(f.byteOrdering, cfieldMinLen)

                      case b if len % 8 != 0 =>
                        b.lowPaddedByteVector(f.byteOrdering)

                      case b => b.bytes
                    }
                    val nextLocal = f.local.copy(sizeBytes =
                      math.min(nextBits.length, f.local.sizeBytes).toInt
                    )
                    val nextField =
                      KnownField(nextLocal, f.byteOrdering, cfield, nextBits)

                    loop(bits.dropRight(len), t, nextField +: result)
                  }
                case None =>
                  loop(bits.dropRight(len), t, result)
              }
          }

        val comp = components.toList.zip(f.field.bits)
        val expanded = DataFields(loop(f.raw.bits, comp, Vector.empty))
        if (expanded.allFields.exists(DynamicField.test))
          field +: expanded.flatMap(expandField(profileMsg, allFields))
        else
          field +: expanded

      case _ => DataFields(Vector(field))
    }

  def decodeFieldWithCodec(
      byteOrdering: ByteOrdering,
      localField: FieldDefinition,
      field: FieldWithCodec[TypedValue[_]]
  ): Decoder[FieldDecodeResult] = {
    val fc = field
      .fieldCodec(localField)(byteOrdering)
      .asDecoder
      .map[FieldDecodeResult.Success](value =>
        FieldDecodeResult.Success(localField, FieldValue(field, value))
      )

    val ac = fixedSizeBytes(
      localField.sizeBytes,
      list(field.fieldCodec(localField)(byteOrdering))
    ).asDecoder
      .map(Nel.unsafeFromList)
      .map(v => ArrayFieldType(v, localField.baseType.fitBaseType))
      .map(v => FieldDecodeResult.Success(localField, FieldValue(field, v)))

    withInvalidValue(localField, byteOrdering) {
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

  def decodeUnknownField(
      byteOrdering: ByteOrdering,
      localField: FieldDefinition
  ): Decoder[FieldDecodeResult] =
    withInvalidValue(localField, byteOrdering) {
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
              byteOrdering,
              localField.baseType.fitBaseType
            )
          )
        } else {
          localResult(localField)(BaseTypedValue.codec(base, byteOrdering))
        }
      }
    }

  private def withInvalidValue(
      localField: FieldDefinition,
      byteOrdering: ByteOrdering
  )(ifValid: Decoder[FieldDecodeResult]): Decoder[FieldDecodeResult] =
    peek(bytes(BaseTypeCodec.length(localField.baseType.fitBaseType))).flatMap { bv =>
      if (BaseTypeCodec.isInvalid(localField.baseType.fitBaseType, byteOrdering)(bv)) {
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

  private object DynamicField {
    def unapply(field: DataField): Option[Nel[Msg.SubField[TypedValue[_]]]] =
      field match {
        case DataField.KnownField(_, _, field: Msg.Field[TypedValue[_]], _) =>
          Nel.fromList(field.subFields().asInstanceOf[List[Msg.SubField[TypedValue[_]]]])
        case _ => None
      }

    def test(field: DataField): Boolean =
      unapply(field).isDefined
  }

  private object ComponentField {
    def unapply(field: DataField): Option[(Nel[String], KnownField)] =
      field match {
        case f @ DataField.KnownField(_, _, field, _) =>
          Nel.fromList(field.components).map(_ -> f)
        case _ => None
      }

    def test(field: DataField): Boolean =
      unapply(field).isDefined
  }

}
