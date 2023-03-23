package fit4s

import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.types.{BaseTypeCodec, TypedValue}

final case class DataDecodeResult(fields: List[FieldDecodeResult]) {
  def findField[A <: TypedValue[_]](ft: Msg.FieldWithCodec[A]): Option[FieldValue[A]] =
    fields.collectFirst {
      case r: FieldDecodeResult.Success if r.fieldValue.field == ft =>
        r.fieldValue.asInstanceOf[FieldValue[A]]
    }

  override def toString = {
    val fieldToString =
      fields
        .map {
          case r: FieldDecodeResult.Success =>
            r.fieldValue.toString
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
