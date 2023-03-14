package fit4s.profile

import scodec.{Codec, Err}
import scodec.codecs._

private[profile] object ScodecTypes {

  private[this] val typeMap: Map[String, Codec[Long]] =
    Map(
      "uint32" -> uint32L,
      "enum" -> ulongL(8),
      "uint16" -> ulongL(16),
      "uint8" -> ulongL(8)
    )

  def baseCodec(name: String): Codec[Long] =
    typeMap.getOrElse(name, fail(Err(s"No base type codec found for name: $name")))
}
