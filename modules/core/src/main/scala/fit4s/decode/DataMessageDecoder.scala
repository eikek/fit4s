package fit4s.decode

import fit4s.FieldDefinition
import fit4s.FitMessage.DataMessage
import fit4s.data.Nel
import fit4s.decode.DataField.KnownField
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import scodec.bits.{BitVector, ByteVector}
import CodecUtils._

import scala.annotation.tailrec

object DataMessageDecoder {

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
    * a single fields contains multiple fields.
    *
    * Sub-fields and top-level fields may have components.
    *
    * Top-level fields may have either sub-fields or components.
    */
  def expandField(profileMsg: Msg, allFields: DataFields)(field: DataField): DataFields =
    field match {
      case DynamicField(subFields) =>
        val activeSubField =
          subFields.toList.find { subField =>
            subField.references.exists { ref =>
              allFields
                .getByName(ref.refField.fieldName)
                .exists(
                  _.decodedValue.fold(
                    _ => false,
                    r => r.asSuccess.map(_.fieldValue.value).contains(ref.refFieldValue)
                  )
                )
            }
          }

        DataFields.of {
          activeSubField
            .map(sf =>
              DataField.KnownField(field.local, field.byteOrdering, sf, field.raw)
            )
            .getOrElse(field)
        }

      case f: DataField.KnownField if f.field.components.nonEmpty =>
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
                    val nextField = KnownField(f.local, f.byteOrdering, cfield, nextBits)

                    loop(bits.dropRight(len), t, nextField +: result)
                  }
                case None =>
                  loop(bits.dropRight(len), t, result)
              }
          }

        val comp = f.field.components.zip(f.field.bits)
        DataFields(loop(f.raw.bits, comp, Vector.empty))

      case _ => DataFields(Vector(field))
    }

  private object DynamicField {
    def unapply(field: DataField): Option[Nel[Msg.SubField[TypedValue[_]]]] =
      field match {
        case DataField.KnownField(_, _, field: Msg.Field[TypedValue[_]], _) =>
          Nel.fromList(field.subFields.asInstanceOf[List[Msg.SubField[TypedValue[_]]]])
        case _ => None
      }
  }
}
