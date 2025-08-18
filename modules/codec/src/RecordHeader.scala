package fit4s.codec

import fit4s.codec.internal.Codecs
import fit4s.codec.internal.RecordCodec

import scodec.Attempt
import scodec.bits.BitVector

sealed trait RecordHeader:
  def localMessageType: Int
  def fold[A](fn: NormalRecordHeader => A, fc: CompressedTimestampHeader => A): A

final case class NormalRecordHeader(messageTypeSpecific: Boolean, localMessageType: Int)
    extends RecordHeader:
  val developerDataFlag: Boolean = messageTypeSpecific
  def fold[A](fn: NormalRecordHeader => A, fc: CompressedTimestampHeader => A): A =
    fn(this)

  def encoded: BitVector =
    RecordCodec.normalHeader.encode(this).require

final case class CompressedTimestampHeader(localMessageType: Int, secondsOffset: Short)
    extends RecordHeader:
  def fold[A](fn: NormalRecordHeader => A, fc: CompressedTimestampHeader => A): A =
    fc(this)

  def encoded: BitVector =
    RecordCodec.compressedTsHeader.encode(this).require

  def resolve(reference: Long): Long =
    val added = (reference & 0xffffffe0) + secondsOffset
    if secondsOffset >= (reference & 0x0000001f) then added
    else added + 0x20

object CompressedTimestampHeader:
  def create(
      localMessageType: Int,
      offset: BitVector
  ): Attempt[CompressedTimestampHeader] =
    val secs = Codecs.ushort5.complete.decodeValue(offset)
    secs.map(CompressedTimestampHeader(localMessageType, _))
