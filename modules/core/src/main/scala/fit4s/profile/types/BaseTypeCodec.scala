package fit4s.profile.types

import fit4s.codecs._
import scodec._
import scodec.bits._

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
    def ulong(bits: Int): PrimCodec = ulongx(bits, _)
    def long(bits: Int): PrimCodec = longx(bits, _)
  }

  def baseCodec(bt: FitBaseType): PrimCodec =
    bt match {
      case FitBaseType.Enum    => PrimCodec.ulong(8)
      case FitBaseType.Sint8   => PrimCodec.long(8)
      case FitBaseType.Uint8   => PrimCodec.ulong(8)
      case FitBaseType.Sint16  => PrimCodec.long(16)
      case FitBaseType.Uint16  => PrimCodec.ulong(16)
      case FitBaseType.Sint32  => PrimCodec.long(32)
      case FitBaseType.Uint32  => PrimCodec.ulong(32)
      case FitBaseType.String  => PrimCodec.ulong(8)
      case FitBaseType.Float32 => PrimCodec.ulong(32) // TODO floatx
      case FitBaseType.Float64 => PrimCodec.ulong(64) // TODO doublex
      case FitBaseType.Uint8z  => PrimCodec.ulong(8)
      case FitBaseType.Uint16z => PrimCodec.ulong(16)
      case FitBaseType.Uint32z => PrimCodec.ulong(32)
      case FitBaseType.Byte    => PrimCodec.ulong(8)
      case FitBaseType.Sint64  => PrimCodec.long(64)
      case FitBaseType.Uint64  => PrimCodec.ulong(64)
      case FitBaseType.Uint64z => PrimCodec.ulong(64)
    }

  def invalidValue(fitBaseType: FitBaseType): ByteVector =
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
}
