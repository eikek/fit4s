package fit4s.decode

import fit4s.FieldDefinition
import fit4s.FitMessage.DataMessage
import fit4s.data.Nel
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import scodec.bits.ByteVector

object DataMessageDecoder {

  def makeDataFields(dm: DataMessage): DataFields = {
    @annotation.tailrec
    def loop(
        fields: List[FieldDefinition],
        bytes: ByteVector,
        result: List[DataField]
    ): List[DataField] =
      fields match {
        case Nil => result.reverse
        case h :: t =>
          val field = dm.definition.profileMsg.flatMap(_.findField(h.fieldDefNum))
          val (raw, next) = bytes.splitAt(h.sizeBytes)
          loop(t, next, DataField(h, dm.definition.archType, field, raw) :: result)
      }

    DataFields(loop(dm.definition.fields, dm.raw, Nil))
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
  def expandField(allFields: DataFields)(field: DataField): DataFields =
    field match {
      case DynamicField(subFields) =>
        val activeSubField =
          subFields.toList.find { subField =>
            subField.references.exists { ref =>
              allFields
                .getByName(ref.refField.fieldName)
                .exists(_.decodedValue.fold(_ => false, _ == ref.refFieldValue))
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
        // needs the mesg!
        ???

      case _ => DataFields(List(field))
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
