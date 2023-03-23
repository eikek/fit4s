package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

final case class FloatTypedValue(rawValue: Double, base: FitBaseType)
    extends TypedValue[Double] {
  override def typeName = base.typeName
}

object FloatTypedValue {
  def codec(bo: ByteOrdering, base: FitBaseType)(implicit
      e: BaseTypeCodec[base.type, Double]
  ): Codec[FloatTypedValue] =
    BaseTypeCodec.baseCodec(base, bo).xmapc(FloatTypedValue(_, base))(_.rawValue)
}
