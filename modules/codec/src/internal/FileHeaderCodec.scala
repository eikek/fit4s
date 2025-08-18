package fit4s.codec
package internal

import scodec.*
import scodec.bits.BitVector
import scodec.bits.hex

private[codec] object FileHeaderCodec:
  private val cc = Codecs
  private val headerSize = cc.ushort8.withContext("Header Size")
  private val fitAscii = cc.constant(hex"2e464954").withContext(".FIT") // .FIT in ascii
  private val protocolVersion = cc.ushort8.withContext("Protocol Version")
  private val profileVersion = cc.uint16L.withContext("Profile Version")
  private val dataSize =
    cc.uint32L.xmap(ByteSize.bytes, _.toBytes).withContext("Data Size")

  private val fields = protocolVersion :: profileVersion :: dataSize <~ fitAscii

  val encoder: Encoder[FileHeader] =
    (headerSize.unit(14) ~> fields).flatAppend(_ => cc.crc).as[FileHeader]

  private val decodeSizeComputeCrc: Decoder[(Short, Int)] =
    new Decoder[(Short, Int)] {
      def decode(bits: BitVector): Attempt[DecodeResult[(Short, Int)]] =
        headerSize.decode(bits).map { result =>
          val size = result.value
          val crc = if (size >= 2) Crc(bits.bytes.take(size - 2)) else 0
          result.copy(value = (size, crc))
        }
    }

  /** Decode a file header and compute the CRC. */
  val decoderWithCrc: Decoder[(FileHeader, Int)] =
    decodeSizeComputeCrc.flatMap { case (size, crcComputed) =>
      val next = size match
        case 12 => fields.flatAppend(_ => cc.provide(0))
        case 14 => fields.flatAppend(_ => cc.crc)
        case n  => cc.fail(DecodeErr.InvalidFitHeaderSize(n))

      next.as[FileHeader].map((_, crcComputed))
    }

  def decoder(checkCrc: Boolean = true): Decoder[FileHeader] =
    decoderWithCrc.flatMap { case (h, crcComputed) =>
      if (checkCrc && h.crc != 0 && h.crc != crcComputed)
        cc.fail(DecodeErr.InvalidHeaderCrc(h.crc, crcComputed))
          .withContext("FileHeader")
          .asDecoder
      else Decoder.pure(h)
    }

  def codec(checkCrc: Boolean = true): Codec[FileHeader] =
    Codec(encoder, decoder(checkCrc))
