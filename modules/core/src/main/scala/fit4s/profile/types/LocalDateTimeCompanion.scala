package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering

trait LocalDateTimeCompanion extends TypedValueCompanion[LocalDateTime] {

  override def codec(bo: ByteOrdering): Codec[LocalDateTime] =
    BaseTypeCodec[FitBaseType.Uint32.type, Long]
      .codec(bo)
      .xmap(LocalDateTime.apply, _.rawValue)
}
