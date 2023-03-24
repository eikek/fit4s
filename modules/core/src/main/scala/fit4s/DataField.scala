package fit4s

import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import scodec.bits.ByteVector

trait DataField {
  def local: FieldDefinition
  def raw: ByteVector
}

object DataField {

  final case class KnownField(
      local: FieldDefinition,
      field: Msg.FieldWithCodec[TypedValue[_]],
      raw: ByteVector
  ) extends DataField

  final case class UnknownField(
      local: FieldDefinition,
      raw: ByteVector
  ) extends DataField

}
