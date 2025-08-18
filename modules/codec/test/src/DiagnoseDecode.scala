package fit4s.codec

import scala.collection.mutable.HashMap

import fit4s.codec.TypedDevField.FieldDescription
import fit4s.codec.internal.Codecs
import fit4s.codec.internal.DecodingContext
import fit4s.codec.internal.FileHeaderCodec
import fit4s.codec.internal.RecordCodec

import scodec.Attempt
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector
import scodec.bits.ByteVector

object DiagnoseDecode:
  private class Context(defs: HashMap[Int, DefinitionMessage]) extends DecodingContext {
    def getDefinition(n: Int): Option[DefinitionMessage] = defs.get(n)
    def getDevFieldDescription(fd: DevFieldDef): Option[FieldDescription] = ???
    def lastTimestamp: Option[Long] = ???
    def put(r: Record): Unit =
      r.fold(dm => defs.put(dm.localMessageType, dm.message), _ => ())
    override def toString(): String = s"Ctx(${defs.keySet})"
  }

  val decoder: Decoder[Unit] =
    val rd = makeRecordDecoder(Context(HashMap.empty))
    for
      header <- diag(FileHeaderCodec.decoder(false))
      dataSize = header.dataSize
      remain <- Decoder.get
      _ <- Decoder.set(remain.take(dataSize.toBits))
      _ <- Codecs.collect[Vector, Record](rd)
    yield ()

  def decode(bv: ByteVector) = decoder.decode(bv.bits)

  private def makeRecordDecoder(ctx: Context) =
    diag(RecordCodec.recordDecoder(ctx).map { r =>
      ctx.put(r)
      r
    })

  def diag[A](d: Decoder[A]): Decoder[A] =
    captureInput(d).map { case (a, bits) =>
      val bb = bits.bytes
      println(s"ByteVector(${bb.length}, ${bb.toHex})\n$a\n\n")
      a
    }

  def captureInput[A](d: Decoder[A]): Decoder[(A, BitVector)] =
    new Decoder[(A, BitVector)] {
      def decode(bits: BitVector): Attempt[DecodeResult[(A, BitVector)]] =
        Codecs.captureInput(d).decode(bits) match
          case r @ Attempt.Successful(_) => r
          case f @ Attempt.Failure(_)    =>
            println("↓↓↓↓↓↓ ERROR BYTES ↓↓↓↓↓↓↓↓↓↓")
            println(bits.bytes.take(50))
            println("↑↑↑↑↑↑ ERROR BYTES ↑↑↑↑↑↑↑↑↑↑ ")
            f
    }
