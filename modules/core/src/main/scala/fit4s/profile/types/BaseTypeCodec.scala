package fit4s.profile.types

import fit4s.codecs._
import scodec.Codec
import scodec.bits.{ByteOrdering, ByteVector}

object BaseTypeCodec {

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

  type PrimCodec = ByteOrdering => Codec[Long]

  object PrimCodec {
    def apply(bits: Int): PrimCodec = ulongx(bits, _)
  }

  // TODO distinguish uintz uint etc
  def baseCodec(bt: FitBaseType): PrimCodec =
    PrimCodec(length(bt) * 8)

  def invalidValue(fitBaseType: FitBaseType): ByteVector =
    fitBaseType match {
      case FitBaseType.Enum    => ByteVector.fromValidHex("ff")
      case FitBaseType.Sint8   => ByteVector.fromValidHex("7f")
      case FitBaseType.Uint8   => ByteVector.fromValidHex("ff")
      case FitBaseType.Sint16  => ByteVector.fromValidHex("7fff")
      case FitBaseType.Uint16  => ByteVector.fromValidHex("ffff")
      case FitBaseType.Sint32  => ByteVector.fromValidHex("7fffffff")
      case FitBaseType.Uint32  => ByteVector.fromValidHex("ffffffff")
      case FitBaseType.String  => ByteVector.fromValidHex("00")
      case FitBaseType.Float32 => ByteVector.fromValidHex("ffffffff")
      case FitBaseType.Float64 => ByteVector.fromValidHex("ffffffffffffffff")
      case FitBaseType.Uint8z  => ByteVector.fromValidHex("00")
      case FitBaseType.Uint16z => ByteVector.fromValidHex("0000")
      case FitBaseType.Uint32z => ByteVector.fromValidHex("00000000")
      case FitBaseType.Byte    => ByteVector.fromValidHex("ff")
      case FitBaseType.Sint64  => ByteVector.fromValidHex("7FFFFFFFFFFFFFFF")
      case FitBaseType.Uint64  => ByteVector.fromValidHex("FFFFFFFFFFFFFFFF")
      case FitBaseType.Uint64z => ByteVector.fromValidHex("0000000000000000")
    }
}
