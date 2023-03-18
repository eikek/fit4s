package fit4s.profile.basetypes

import fit4s.profile.GenBaseTypeCompanion
import scodec.Codec
import scodec.bits.ByteOrdering

trait DateTimeCompanion extends GenBaseTypeCompanion[DateTime] {

  override def codec(bo: ByteOrdering): Codec[DateTime] =
    BaseTypeCodec
      .baseCodec(FitBaseType.Uint32)(bo)
      .xmap(DateTime.apply, _.rawValue)

  protected val allMap: Map[DateTime, Long] = Map.empty

  val all: List[DateTime] = Nil
}
