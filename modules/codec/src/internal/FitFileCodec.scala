package fit4s.codec
package internal

import fit4s.codec.internal.Codecs.captureInput

import scodec.Attempt
import scodec.Codec
import scodec.DecodeResult
import scodec.Decoder
import scodec.Encoder
import scodec.bits.BitVector
import scodec.bits.ByteVector

private[codec] object FitFileCodec:
  private def decoder(ctx: DecodingContext.Ctx): Decoder[(Record, DecodingContext.Ctx)] =
    RecordCodec.recordDecoder(ctx).map(r => (r, ctx.update(r)))

  /** Decodes records until no bits remain. */
  private val recordsDecoder0: Decoder[Vector[Record]] =
    new Decoder[Vector[Record]] {
      def decode(bits: BitVector): Attempt[DecodeResult[Vector[Record]]] =
        decode0(bits, DecodingContext(), Vector.empty).map(rs =>
          DecodeResult(rs, BitVector.empty)
        )

      @annotation.tailrec
      private def decode0(
          bits: BitVector,
          ctx: DecodingContext.Ctx,
          result: Vector[Record]
      ): Attempt[Vector[Record]] =
        if (bits.isEmpty) Attempt.successful(result)
        else
          decoder(ctx).decode(bits) match
            case Attempt.Successful(DecodeResult((r, ctxN), remain)) =>
              decode0(remain, ctxN, r +: result)
            case f @ Attempt.Failure(_) => f
    }

  def recordsDecoder(size: ByteSize): Decoder[Vector[Record]] =
    for
      current <- Decoder.get
      (hd, tl) = current.splitAt(size.toBits)
      _ <- Decoder.set(hd)
      r <- recordsDecoder0
      _ <- Decoder.set(tl)
    yield r

  private def headerAndRecordsDecoder(checkCrc: Boolean) =
    Codecs.captureInput {
      for
        header <- FileHeaderCodec.decoder(checkCrc)
        rs <- recordsDecoder(header.dataSize)
      yield (header, rs)
    }

  def fitFileDecoder(checkCrc: Boolean): Decoder[FitFile] =
    for
      headerAndRecords <- headerAndRecordsDecoder(checkCrc)
      ((h, rs), bits) = headerAndRecords
      crcProvided <- Codecs.crc.asDecoder
      crcComputed = Crc(bits.bytes)
      res <-
        if (checkCrc && crcProvided != crcComputed)
          Decoder.liftAttempt(
            Attempt.failure(DecodeErr.InvalidContentCrc(crcProvided, crcComputed))
          )
        else Decoder.pure(rs)
    yield FitFile(h, rs, crcProvided)

  def fitFilesDecoder(checkCrc: Boolean): Decoder[Vector[FitFile]] =
    Codecs.collect[Vector, FitFile](fitFileDecoder(checkCrc))

  private val recordsCodecForEncoding =
    scodec.codecs.vector(RecordCodec.record(DecodingContext.empty))

  val fitFileEncoderRaw: Encoder[FitFile] =
    val records = recordsCodecForEncoding
    (FileHeaderCodec.codec() :: records :: Codecs.crc).as[FitFile]

  val fitFileEncoder: Encoder[FitFile] =
    Encoder[FitFile] { fit =>
      for
        content <- recordsCodecForEncoding.encode(fit.records)
        contentBytes = content.bytes
        crc = Crc(contentBytes)
        header = fit.header.copy(dataSize = ByteSize.bytes(contentBytes.length)).updateCrc
        headerBits <- FileHeaderCodec.encoder.encode(header)
        crcBits <- Codecs.crc.encode(crc)
      yield headerBits ++ content ++ crcBits
    }

  val fitStructureCodec: Codec[FitFileStructure] = {
    val encoder =
      (Codecs.bytes :: Codecs.bytes :: Codecs.bytes).contramap[FitFileStructure](f =>
        (f.headerBytes, f.records, f.crcBytes)
      )
    val decoder = for
      fileHeader <- captureInput(FileHeaderCodec.decoder(false))
      (header, hb) = fileHeader
      rs <- Codecs.bytes(header.dataSize.toBytes.toInt)
      crcBytes <- Codecs.bytes(2)
      crc <- Decoder.liftAttempt(Codecs.crc.complete.decodeValue(crcBytes.bits))
    yield FitFileStructure(header, hb.bytes, rs, crc, crcBytes)
    Codec(encoder, decoder)
  }

  def codec(checkCrc: Boolean, rawEncode: Boolean): Codec[FitFile] =
    Codec(
      if rawEncode then fitFileEncoderRaw else fitFileEncoder,
      fitFileDecoder(checkCrc)
    )
