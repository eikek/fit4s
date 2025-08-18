package fit4s.codec

import scodec.bits.ByteOrdering

final case class DefinitionMessage(
    meta: DefinitionMessage.Meta,
    fields: List[FieldDef],
    devFields: List[DevFieldDef]
):
  val fieldsSize: ByteSize =
    fields.map(_.size).sum

  val devFieldsSize: ByteSize =
    devFields.map(_.size).sum

  val dataMessageSize: ByteSize =
    fieldsSize + devFieldsSize

  override def toString(): String =
    s"DefinitionMsg(${meta.byteOrder}, " +
      s"mesgNum=${meta.globalMessageNum}, " +
      s"fields=${fields.size}, " +
      s"devFields=${devFields.size}, " +
      s"dataSize=$dataMessageSize)"

object DefinitionMessage:

  final case class Meta(
      byteOrder: ByteOrdering,
      globalMessageNum: Int
  )
