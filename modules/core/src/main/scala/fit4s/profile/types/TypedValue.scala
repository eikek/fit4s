package fit4s.profile.types

import scodec.Codec
import scodec.bits.{ByteOrdering, ByteVector}
import scodec.codecs._

trait TypedValue {

  def rawValue: Long

  def typeName: String

}

trait TypedValueCompanion[A <: TypedValue] {
  def codec(bo: ByteOrdering): Codec[A]

  protected def baseType: FitBaseType

  lazy val invalidValue: ByteVector = BaseTypeCodec.invalidValue(baseType)
}

trait EnumValueCompanion[A <: TypedValue] extends TypedValueCompanion[A] {
  final def codec(bo: ByteOrdering): Codec[A] =
    mappedEnum[A, Long](BaseTypeCodec.baseCodec(baseType)(bo), allMap)

  def all: List[A]

  protected def allMap: Map[A, Long]

  def byRawValue(n: Long): Option[A] =
    all.find(_.rawValue == n)

  def byOrdinal(n: Int): Option[A] =
    all.lift.apply(n)

}
