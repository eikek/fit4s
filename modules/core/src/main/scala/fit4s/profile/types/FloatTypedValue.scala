package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

final class FloatTypedValue(val rawValue: Double, base: FitBaseType)
    extends TypedValue[Double] {
  override def typeName = base.typeName
}

object FloatTypedValue {

  def apply(n: Float): FloatTypedValue = new FloatTypedValue(n, FitBaseType.Float32)
  def apply(n: Double): FloatTypedValue = new FloatTypedValue(n, FitBaseType.Float64)

  def codec(bo: ByteOrdering, base: FitBaseType)(implicit
      e: BaseTypeCodec[base.type, Double]
  ): Codec[FloatTypedValue] =
    BaseTypeCodec.baseCodec(base, bo).xmap(FloatTypedValue.apply, _.rawValue)
}
