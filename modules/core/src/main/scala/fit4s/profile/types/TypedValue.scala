package fit4s.profile.types

import scodec.Codec
import scodec.bits.{ByteOrdering, ByteVector}
import scodec.codecs._

trait TypedValue[V] {

  def rawValue: V

  def typeName: String

}

object TypedValue {
  def codec(bt: FitBaseType, bo: ByteOrdering): Codec[TypedValue[_]] =
    (bt match {
      case FitBaseType.Enum =>
        LongTypedValue.codec(bo, FitBaseType.Enum)
      case FitBaseType.Sint8 =>
        LongTypedValue.codec(bo, FitBaseType.Sint8)
      case FitBaseType.Uint8 =>
        LongTypedValue.codec(bo, FitBaseType.Uint8)
      case FitBaseType.Sint16 =>
        LongTypedValue.codec(bo, FitBaseType.Sint16)
      case FitBaseType.Uint16 =>
        LongTypedValue.codec(bo, FitBaseType.Uint16)
      case FitBaseType.Sint32 =>
        LongTypedValue.codec(bo, FitBaseType.Sint32)
      case FitBaseType.Uint32 =>
        LongTypedValue.codec(bo, FitBaseType.Uint32)
      case FitBaseType.String =>
        StringTypedValue.codec(BaseTypeCodec.length(bt))
      case FitBaseType.Float32 =>
        FloatTypedValue.codec(bo, FitBaseType.Float32)
      case FitBaseType.Float64 =>
        FloatTypedValue.codec(bo, FitBaseType.Float64)
      case FitBaseType.Uint8z =>
        LongTypedValue.codec(bo, FitBaseType.Uint8z)
      case FitBaseType.Uint16z =>
        LongTypedValue.codec(bo, FitBaseType.Uint16z)
      case FitBaseType.Uint32z =>
        LongTypedValue.codec(bo, FitBaseType.Uint32z)
      case FitBaseType.Byte =>
        LongTypedValue.codec(bo, FitBaseType.Byte)
      case FitBaseType.Sint64 =>
        LongTypedValue.codec(bo, FitBaseType.Sint64)
      case FitBaseType.Uint64 =>
        LongTypedValue.codec(bo, FitBaseType.Uint64)
      case FitBaseType.Uint64z =>
        LongTypedValue.codec(bo, FitBaseType.Uint64)
    }).asInstanceOf[Codec[TypedValue[_]]]

}

trait TypedValueCompanion[A <: TypedValue[_]] {
  def codec(bo: ByteOrdering): Codec[A]

  protected def baseType: FitBaseType

  lazy val invalidValue: ByteVector = BaseTypeCodec.invalidValue(baseType)
}

trait EnumValueCompanion[A <: TypedValue[_]] extends TypedValueCompanion[A] {

  final def codec(bo: ByteOrdering): Codec[A] =
    mappedEnum[A, Long](baseTypeCodec(bo), allMap)

  def all: List[A]

  protected def allMap: Map[A, Long]

  protected def baseTypeCodec(bo: ByteOrdering): Codec[Long]

  def byRawValue(n: Long): Option[A] =
    all.find(_.rawValue == n)

  def byOrdinal(n: Int): Option[A] =
    all.lift.apply(n)
}
