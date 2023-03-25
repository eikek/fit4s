package fit4s.decode

import fit4s.FieldDefinition
import fit4s.decode.DataField.{KnownField, UnknownField}
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import scodec.bits.ByteVector

trait DataField {
  def local: FieldDefinition
  def raw: ByteVector
  def fold[A](fa: KnownField => A, fb: UnknownField => A): A
}

object DataField {

  def apply(
      local: FieldDefinition,
      field: Msg.FieldWithCodec[TypedValue[_]],
      raw: ByteVector
  ): DataField =
    KnownField(local, field, raw)

  def apply(local: FieldDefinition, raw: ByteVector): DataField =
    UnknownField(local, raw)

  def apply(
      local: FieldDefinition,
      maybeField: Option[Msg.FieldWithCodec[TypedValue[_]]],
      raw: ByteVector
  ): DataField =
    maybeField.map(apply(local, _, raw)).getOrElse(apply(local, raw))

  final case class KnownField(
      local: FieldDefinition,
      field: Msg.FieldWithCodec[TypedValue[_]],
      raw: ByteVector
  ) extends DataField {
    def fold[A](fa: KnownField => A, fb: UnknownField => A): A = fa(this)
  }

  final case class UnknownField(
      local: FieldDefinition,
      raw: ByteVector
  ) extends DataField {
    def fold[A](fa: KnownField => A, fb: UnknownField => A): A = fb(this)
  }
}
