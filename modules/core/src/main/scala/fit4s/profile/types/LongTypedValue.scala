package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

final case class LongTypedValue(rawValue: Long, fitBaseType: FitBaseType)
    extends TypedValue[Long] {

  override def typeName = "long"
}

object LongTypedValue {

  def codec(bo: ByteOrdering, base: FitBaseType): Codec[LongTypedValue] =
    BaseTypeCodec.baseCodec(base)(bo).xmap(LongTypedValue(_, base), _.rawValue)
}
