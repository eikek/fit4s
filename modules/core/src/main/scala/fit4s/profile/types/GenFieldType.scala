package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

trait GenFieldType {

  def rawValue: Long

  def typeName: String

}

trait GenFieldTypeCompanion[A <: GenFieldType] {
  def codec(bo: ByteOrdering): Codec[A] =
    mappedEnum[A, Long](BaseTypeCodec.baseCodec(baseType)(bo), allMap)

  def all: List[A]

  protected def allMap: Map[A, Long]

  protected def baseType: FitBaseType

  def byRawValue(n: Long): Option[A] =
    all.find(_.rawValue == n)

  def byOrdinal(n: Int): Option[A] =
    all.lift.apply(n)

  def byTypeName(name: String): Option[A] =
    all.find(_.typeName == name)
}
