package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

trait LocalDateTimeCompanion extends TypedValueCompanion[LocalDateTime] {

  override def codec(bo: ByteOrdering): Codec[LocalDateTime] =
    BaseTypeCodec
      .baseCodec(FitBaseType.Uint32)(bo)
      .xmap(LocalDateTime.apply, _.rawValue)
}
