package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

final case class LongFieldType(rawValue: Long, fitBaseType: FitBaseType)
    extends TypedValue {

  override def typeName = "long"
}

object LongFieldType {

  def codec(bo: ByteOrdering, base: FitBaseType): Codec[LongFieldType] =
    BaseTypeCodec.baseCodec(base)(bo).xmap(LongFieldType(_, base), _.rawValue)
}
