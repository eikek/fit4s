package fit4s.profile.basetypes

import fit4s.profile.GenBaseType
import scodec.Codec
import scodec.codecs._

final case class StringBaseType(value: String) extends GenBaseType {
  override def rawValue = value.hashCode.toLong // todo :-)

  override def typeName = "string"
}

object StringBaseType {
  def codec(sizeBytes: Int): Codec[StringBaseType] =
    fixedSizeBytes(sizeBytes, cstring).xmap(StringBaseType.apply, _.value)
}
