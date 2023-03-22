package fit4s.profile.types

import scodec.Codec
import scodec.codecs._

final case class StringTypedValue(rawValue: String) extends TypedValue[String] {

  override def typeName = "string"
}

object StringTypedValue {
  def codec(sizeBytes: Int): Codec[StringTypedValue] =
    fixedSizeBytes(sizeBytes, cstring).xmap(StringTypedValue.apply, _.rawValue)

  val baseType: FitBaseType = FitBaseType.String
}
