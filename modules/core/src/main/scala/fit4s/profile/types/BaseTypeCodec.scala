package fit4s.profile.types

import scodec.Codec
import scodec.codecs._
import scodec.bits._
import fit4s.util.Codecs._

trait BaseTypeCodec[T <: FitBaseType, R] {
  def codec(byteOrdering: ByteOrdering): Codec[R]
}

object BaseTypeCodec {
  def apply[T <: FitBaseType, R](implicit e: BaseTypeCodec[T, R]): BaseTypeCodec[T, R] = e

  def instance[T <: FitBaseType, R](f: ByteOrdering => Codec[R]): BaseTypeCodec[T, R] =
    (byteOrdering: ByteOrdering) => f(byteOrdering)

  implicit val enumBaseTypeCodec: BaseTypeCodec[FitBaseType.Enum.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Enum.type, Long](ulongx(8, _))

  implicit val sint8BaseTypeCodec: BaseTypeCodec[FitBaseType.Sint8.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Sint8.type, Long](longx(8, _))

  implicit val uint8BaseTypeCodec: BaseTypeCodec[FitBaseType.Uint8.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint8.type, Long](ulongx(8, _))

  implicit val sint16BaseTypeCodec: BaseTypeCodec[FitBaseType.Sint16.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Sint16.type, Long](longx(16, _))

  implicit val uint16BaseTypeCodec: BaseTypeCodec[FitBaseType.Uint16.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint16.type, Long](ulongx(16, _))

  implicit val sint32BaseTypeCodec: BaseTypeCodec[FitBaseType.Sint32.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Sint32.type, Long](longx(32, _))

  implicit val uint32BaseTypeCodec: BaseTypeCodec[FitBaseType.Uint32.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint32.type, Long](ulongx(32, _))

  implicit val stringBaseTypeCodec: BaseTypeCodec[FitBaseType.String.type, String] =
    BaseTypeCodec.instance[FitBaseType.String.type, String](_ => cstring)

  implicit val float32BaseTypeCodec: BaseTypeCodec[FitBaseType.Float32.type, Double] =
    BaseTypeCodec.instance[FitBaseType.Float32.type, Double](floatx)

  implicit val float64BaseTypeCodec: BaseTypeCodec[FitBaseType.Float64.type, Double] =
    BaseTypeCodec.instance[FitBaseType.Float64.type, Double](doublex)

  implicit val uint8zBaseTypeCodec: BaseTypeCodec[FitBaseType.Uint8z.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint8z.type, Long](ulongx(8, _))

  implicit val uint16zBaseTypeCodec: BaseTypeCodec[FitBaseType.Uint16z.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint16z.type, Long](ulongx(16, _))

  implicit val uint32zBaseTypeCodec: BaseTypeCodec[FitBaseType.Uint32z.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint32z.type, Long](ulongx(32, _))

  implicit val byteBaseTypeCodec: BaseTypeCodec[FitBaseType.Byte.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Byte.type, Long](ulongx(8, _))

  implicit val sint64BaseTypeCodec: BaseTypeCodec[FitBaseType.Sint64.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Sint64.type, Long](longx(64, _))

  implicit val uint64BaseTypeCodec: BaseTypeCodec[FitBaseType.Uint64.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint64.type, Long](ulongx(64, _))

  implicit val uint64zBaseTypeCodec: BaseTypeCodec[FitBaseType.Uint64z.type, Long] =
    BaseTypeCodec.instance[FitBaseType.Uint64z.type, Long](ulongx(64, _))

  def length(bt: FitBaseType): Int =
    bt match {
      case FitBaseType.Enum    => 1
      case FitBaseType.Sint8   => 1
      case FitBaseType.Uint8   => 1
      case FitBaseType.Sint16  => 2
      case FitBaseType.Uint16  => 2
      case FitBaseType.Sint32  => 4
      case FitBaseType.Uint32  => 4
      case FitBaseType.String  => 1
      case FitBaseType.Float32 => 4
      case FitBaseType.Float64 => 8
      case FitBaseType.Uint8z  => 1
      case FitBaseType.Uint16z => 2
      case FitBaseType.Uint32z => 4
      case FitBaseType.Byte    => 1
      case FitBaseType.Sint64  => 8
      case FitBaseType.Uint64  => 8
      case FitBaseType.Uint64z => 8
    }

  def baseCodec[V](bt: FitBaseType, bo: ByteOrdering)(implicit
      e: BaseTypeCodec[bt.type, V]
  ): Codec[V] =
    e.codec(bo)

  private def invalidValue(fitBaseType: FitBaseType): ByteVector =
    fitBaseType match {
      case FitBaseType.Enum    => hex"ff"
      case FitBaseType.Sint8   => hex"7f"
      case FitBaseType.Uint8   => hex"ff"
      case FitBaseType.Sint16  => hex"7fff"
      case FitBaseType.Uint16  => hex"ffff"
      case FitBaseType.Sint32  => hex"7fffffff"
      case FitBaseType.Uint32  => hex"ffffffff"
      case FitBaseType.String  => hex"00"
      case FitBaseType.Float32 => hex"ffffffff"
      case FitBaseType.Float64 => hex"ffffffffffffffff"
      case FitBaseType.Uint8z  => hex"00"
      case FitBaseType.Uint16z => hex"0000"
      case FitBaseType.Uint32z => hex"00000000"
      case FitBaseType.Byte    => hex"ff"
      case FitBaseType.Sint64  => hex"7FFFFFFFFFFFFFFF"
      case FitBaseType.Uint64  => hex"FFFFFFFFFFFFFFFF"
      case FitBaseType.Uint64z => hex"0000000000000000"
    }

  def isInvalid(fitBaseType: FitBaseType, byteOrdering: ByteOrdering)(
      bv: ByteVector
  ): Boolean =
    if (byteOrdering == ByteOrdering.BigEndian) invalidValue(fitBaseType) == bv
    else invalidValue(fitBaseType).reverse == bv
}
