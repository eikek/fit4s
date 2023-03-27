package fit4s.decode

import fit4s.decode.DataField.{KnownField, UnknownField}
import fit4s.profile.messages.Msg
import fit4s.profile.types.TypedValue
import fit4s.{FieldDecodeResult, FieldDefinition}
import scodec.{Attempt, Decoder}
import scodec.bits.{ByteOrdering, ByteVector}

trait DataField {
  def local: FieldDefinition
  def byteOrdering: ByteOrdering
  def raw: ByteVector
  def fold[A](fa: KnownField => A, fb: UnknownField => A): A
  def decodedValue: Attempt[FieldDecodeResult]
}

object DataField {

  def apply(
      local: FieldDefinition,
      byteOrdering: ByteOrdering,
      field: Msg.FieldWithCodec[TypedValue[_]],
      raw: ByteVector
  ): DataField =
    KnownField(local, byteOrdering, field, raw)

  def apply(
      local: FieldDefinition,
      byteOrdering: ByteOrdering,
      raw: ByteVector
  ): DataField =
    UnknownField(local, byteOrdering, raw)

  def apply(
      local: FieldDefinition,
      byteOrdering: ByteOrdering,
      maybeField: Option[Msg.FieldWithCodec[TypedValue[_]]],
      raw: ByteVector
  ): DataField =
    maybeField
      .map(apply(local, byteOrdering, _, raw))
      .getOrElse(apply(local, byteOrdering, raw))

  final case class KnownField(
      local: FieldDefinition,
      byteOrdering: ByteOrdering,
      field: Msg.FieldWithCodec[TypedValue[_]],
      raw: ByteVector
  ) extends DataField {
    def fold[A](fa: KnownField => A, fb: UnknownField => A): A = fa(this)

    private val fieldDecoder: Decoder[FieldDecodeResult] =
      DataMessageDecoder.decodeFieldWithCodec(byteOrdering, local, field)

    lazy val decodedValue = fieldDecoder.complete.decode(raw.bits).map(_.value)
  }

  final case class UnknownField(
      local: FieldDefinition,
      byteOrdering: ByteOrdering,
      raw: ByteVector
  ) extends DataField {
    def fold[A](fa: KnownField => A, fb: UnknownField => A): A = fb(this)

    private val fieldDecoder: Decoder[FieldDecodeResult] =
      DataMessageDecoder.decodeUnknownField(byteOrdering, local)

    lazy val decodedValue = fieldDecoder.complete.decode(raw.bits).map(_.value)
  }
}
