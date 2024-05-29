package fit4s.decode

import scodec.bits.{BitVector, ByteOrdering, ByteVector}

private[decode] trait CodecUtils:

  implicit final class BitVectorOps(self: BitVector):
    def lowPaddedByteVector(byteOrdering: ByteOrdering): ByteVector =
      if (byteOrdering == ByteOrdering.BigEndian)
        (ByteVector.low(1).bits.take(8 - (self.size % 8)) ++ self).bytes
      else
        (self ++ ByteVector.low(1).bits.take(8 - (self.size % 8))).bytes

    def lowPaddedByteVectorToLength(
        byteOrdering: ByteOrdering,
        targetLen: Long
    ): ByteVector =
      if (byteOrdering == ByteOrdering.BigEndian)
        (BitVector.low(targetLen - self.size) ++ self).bytes
      else
        (self ++ BitVector.low(targetLen - self.size)).bytes

private[decode] object CodecUtils extends CodecUtils
