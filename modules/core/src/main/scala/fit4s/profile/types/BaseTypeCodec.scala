package fit4s.profile.types

import fit4s.codecs._
import scodec.Codec
import scodec.bits.ByteOrdering

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
}
