package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

final case class StringTypedValue(rawValue: String) extends TypedValue[String] {

  override def typeName = "string"
}

object StringTypedValue {
  def codec(sizeBytes: Int): Codec[StringTypedValue] = {
    val sc = BaseTypeCodec.baseCodec(FitBaseType.String, ByteOrdering.LittleEndian)
    fixedSizeBytes(sizeBytes, sc).xmap(StringTypedValue.apply, _.rawValue)
  }

  val baseType: FitBaseType = FitBaseType.String
}
