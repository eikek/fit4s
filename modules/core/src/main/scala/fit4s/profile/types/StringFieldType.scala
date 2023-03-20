package fit4s.profile.types

import scodec.Codec
import scodec.codecs._

final case class StringFieldType(value: String) extends GenFieldType {
  override def rawValue = value.hashCode.toLong // TODO :-)

  override def typeName = "string"
}

object StringFieldType {
  def codec(sizeBytes: Int): Codec[StringFieldType] =
    fixedSizeBytes(sizeBytes, cstring).xmap(StringFieldType.apply, _.value)

  val baseType: FitBaseType = FitBaseType.String
}
