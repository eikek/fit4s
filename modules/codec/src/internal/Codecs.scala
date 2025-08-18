package fit4s.codec
package internal

import scala.collection.Factory

import scodec.Attempt
import scodec.Codec
import scodec.DecodeResult
import scodec.Decoder
import scodec.Err
import scodec.bits.BitVector
import scodec.bits.ByteOrdering
import scodec.codecs as c

private[codec] object Codecs:
  export c.listOfN, c.bytes, c.provide, c.uint2, c.bool, c.ushort8, c.constant, c.uint16L,
    c.uint32L, c.vector, c.fixedSizeBytes

  val crc = c.uint16L.withContext("CRC")
  val reservedBit = c.bool.unit(false).withContext("Reserved")
  def reservedBits(n: Int) = c.bool(n).unit(false).withContext("Reserved")
  val reservedByte = reservedBits(8)
  val byteOrder: Codec[ByteOrdering] = c.uint8.xmap(
    n => if (n == 0) ByteOrdering.LittleEndian else ByteOrdering.BigEndian,
    bo => if (bo == ByteOrdering.LittleEndian) 0 else 1
  )
  val ushort5 = c.ushort(5)

  val localMessageType = c.uint4L.withContext("local messsage type")
  val headerType = c.bool.xmap(HeaderType.fromBool, _.toBool)
  val msgType = c.bool.xmap(MsgType.fromBool, _.toBool)
  val peekMsgType = c.peek(c.bits(2)).map { bits =>
    HeaderType
      .fromBool(bits.head)
      .fold(MsgType.fromBool(bits.last), MsgType.Data)
  }

  def fail[A](err: DecodeErr) = c.fail[A](err)

  def globalMsgNum(bo: ByteOrdering) = bo.fold(c.uint16, c.uint16L)
  def fieldCount(bo: ByteOrdering) = bo.fold(c.uint8, c.uint8L)
  def fieldDefNum(bo: ByteOrdering) = bo.fold(c.uint8, c.uintL(8))
  def fieldSize(bo: ByteOrdering) = bo.fold(c.ushort8, c.ushortL(8))
  def devDataIdx(bo: ByteOrdering) = bo.fold(c.ushort8, c.ushortL(8))
  def baseTypeNum(bo: ByteOrdering) = c.ushortL(5)

  val stringUtf8 = c.nulTerminatedString(c.utf8)

  def captureInput[A](d: Decoder[A]): Decoder[(A, BitVector)] =
    for
      before <- Decoder.get
      result <- d
      after <- Decoder.get
    yield (result, before.take(before.length - after.length))

  def collect[F[_], A](d: Decoder[A])(using
      factory: Factory[A, F[A]]
  ): Decoder[F[A]] =
    new Decoder[F[A]] {
      def decode(bits: BitVector): Attempt[DecodeResult[F[A]]] =
        d.collect[F, A](bits, None)
    }

  def collectUntilErr[F[_], A](d: Decoder[A], buffer: BitVector)(using
      factory: Factory[A, F[A]]
  ): Attempt[DecodeResult[F[A]]] =
    val bldr = factory.newBuilder
    var remaining = buffer
    var count = 0
    var error: Option[Err] = None
    var running = true
    while running && remaining.nonEmpty do
      d.decode(remaining) match
        case Attempt.Successful(DecodeResult(value, rest)) =>
          bldr += value
          count += 1
          remaining = rest
        case Attempt.Failure(e: Err.InsufficientBits) =>
          running = false

        case Attempt.Failure(err) =>
          error = Some(err.pushContext(count.toString))
          remaining = BitVector.empty
    Attempt.fromErrOption(error, DecodeResult(bldr.result, remaining))

  extension (bo: ByteOrdering)
    def fold[A](be: => A, le: => A): A =
      bo match
        case ByteOrdering.BigEndian    => be
        case ByteOrdering.LittleEndian => le

    def padTo(bits: BitVector, n: Long): BitVector =
      fold(bits.padLeft(n), bits.padRight(n))
