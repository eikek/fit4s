package fit4s.profile

import scodec.Codec
import scodec.codecs._

trait GenBaseType {

  def rawValue: Long

}

trait GenBaseTypeCompanion[A <: GenBaseType] {
  def codec: Codec[A] =
    mappedEnum[A, Long](ScodecTypes.baseCodec(baseCodecName), allMap)

  def all: List[A]

  protected def allMap: Map[A, Long]

  protected def baseCodecName: String

  def byRawValue(n: Long): Option[A] =
    all.find(_.rawValue == n)

  def byOrdinal(n: Int): Option[A] =
    all.lift.apply(n)
}
