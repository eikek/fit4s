package fit4s.profile

import fit4s.profile.basetypes.{BaseTypeCodec, FitBaseType, MesgNum}
import scodec.Codec
import scodec.bits.ByteOrdering

trait Msg {

  def globalMessageNumber: MesgNum

  def allFields: List[Msg.Field[_]]
}

object Msg {

  final case class Field[A <: GenBaseType](
      fieldDefinitionNumber: Int,
      fieldName: String,
      fieldTypeName: String,
      fieldBaseType: FitBaseType,
      fieldCodec: ByteOrdering => Codec[A]
  ) {
    lazy val baseTypeLen: Int = BaseTypeCodec.length(fieldBaseType)
    lazy val baseTypeCodec: ByteOrdering => Codec[Long] =
      BaseTypeCodec.baseCodec(fieldBaseType)
  }
}
