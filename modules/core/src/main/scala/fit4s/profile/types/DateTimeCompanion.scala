package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

trait DateTimeCompanion extends TypedValueCompanion[DateTime] {

  override def codec(bo: ByteOrdering): Codec[DateTime] =
    BaseTypeCodec
      .baseCodec(FitBaseType.Uint32)(bo)
      .xmap(DateTime.apply, _.rawValue)
}
