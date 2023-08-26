package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

trait TypedValue[V] {

  def rawValue: V

  def typeName: String

}

trait TypedValueCompanion[A <: TypedValue[_]] {
  def codec(bo: ByteOrdering): Codec[A]

  protected def baseType: FitBaseType
}

trait EnumValueCompanion[A <: TypedValue[_]] extends TypedValueCompanion[A] {

  final def codec(bo: ByteOrdering): Codec[A] =
    mappedEnum[A, Long](baseTypeCodec(bo), allMap)

  def all: List[A]

  protected def allMap: Map[A, Long]

  protected def baseTypeCodec(bo: ByteOrdering): Codec[Long]

  def byRawValue(n: Long): Option[A] =
    all.find(_.rawValue == n)

  def byTypeName(tn: String): Option[A] =
    all.find(_.typeName.equalsIgnoreCase(tn))

  def unsafeByRawValue(n: Long): A =
    byRawValue(n).getOrElse(sys.error(s"No sport found for value: $n"))

  def byOrdinal(n: Int): Option[A] =
    all.lift.apply(n)
}
