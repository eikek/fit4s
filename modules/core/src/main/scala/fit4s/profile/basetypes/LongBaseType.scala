package fit4s.profile.basetypes

import fit4s.profile.GenBaseType
import scodec.Codec
import scodec.bits.ByteOrdering

final case class LongBaseType(rawValue: Long, fitBaseType: FitBaseType)
    extends GenBaseType {

  override def typeName = "long"
}

object LongBaseType {

  def codec(bo: ByteOrdering, base: FitBaseType): Codec[LongBaseType] =
    BaseTypeCodec.baseCodec(base)(bo).xmap(LongBaseType(_, base), _.rawValue)
}
