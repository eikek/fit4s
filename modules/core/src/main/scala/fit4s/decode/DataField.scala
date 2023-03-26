package fit4s.decode

import fit4s.data.Nel
import fit4s.{FieldDecodeResult, FieldDefinition}
import fit4s.decode.DataField.{KnownField, UnknownField}
import fit4s.profile.FieldValue
import fit4s.profile.messages.Msg
import fit4s.profile.messages.Msg.ArrayDef
import fit4s.profile.types.{ArrayFieldType, TypedValue}
import scodec.Decoder
import scodec.bits.{ByteOrdering, ByteVector}
import scodec.codecs.{fixedSizeBytes, list}

trait DataField {
  def local: FieldDefinition
  def byteOrdering: ByteOrdering
  def raw: ByteVector
  def fold[A](fa: KnownField => A, fb: UnknownField => A): A
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

    private val fieldDecoder: Decoder[FieldDecodeResult] = {
      val fc = field
        .fieldCodec(local)(byteOrdering)
        .asDecoder
        .map[FieldDecodeResult.Success](value =>
          FieldDecodeResult.Success(local, FieldValue(field, value))
        )

      val ac = fixedSizeBytes(
        local.sizeBytes,
        list(field.fieldCodec(local)(byteOrdering))
      ).asDecoder
        .map(Nel.unsafeFromList)
        .map(v => ArrayFieldType(v, local.baseType.fitBaseType))
        .map(v => FieldDecodeResult.Success(local, FieldValue(field, v)))

      field.isArray match {
        case ArrayDef.NoArray =>
          fc.asDecoder

        case ArrayDef.DynamicSize =>
          if (local.sizeBytes > field.baseTypeLen) ac else fc

        case ArrayDef.Sized(n) =>
          if (n > 1) ac else fc
      }
    }

    lazy val decodedValue = fieldDecoder.complete.decode(raw.bits).map(_.value)
  }

  final case class UnknownField(
      local: FieldDefinition,
      byteOrdering: ByteOrdering,
      raw: ByteVector
  ) extends DataField {
    def fold[A](fa: KnownField => A, fb: UnknownField => A): A = fb(this)
  }
}
