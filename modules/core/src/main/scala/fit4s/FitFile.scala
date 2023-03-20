package fit4s

import fit4s.profile.types.MesgNum
import scodec.{Attempt, Codec, DecodeResult, Encoder, SizeBound}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

final case class FitFile(
    header: FileHeader,
    records: List[Record],
    crc: Int
) {

  def findData(n: MesgNum): List[FitMessage.DataMessage] =
    records.collect {
      case Record.DataRecord(_, cnt) if cnt.definition.isMesgNum(n) =>
        cnt
    }

  def dataRecords: List[FitMessage.DataMessage] =
    records.collect { case Record.DataRecord(_, cnt) =>
      cnt
    }
}

object FitFile {

  def codec: Codec[FitFile] =
    FileHeader.codec
      .flatPrepend { header =>
        fixedSizeBytes(header.dataSize, recordsCodec.withContext("Record")) :: uint16L
      }
      .as[FitFile]

  def decode(bv: ByteVector): Attempt[FitFile] =
    codec.complete.decode(bv.bits).map(_.value)

  def decodeUnsafe(bv: ByteVector): FitFile =
    decode(bv).fold(
      err => sys.error(s"Decoding FIT failed: ${err.messageWithContext}"),
      identity
    )

  // TODO improve by using a map of definition messages instead of list
  private def recordsCodec: Codec[List[Record]] =
    new Codec[List[Record]] {
      override def decode(bits: BitVector): Attempt[DecodeResult[List[Record]]] = {
        @annotation.tailrec
        def go(
            prev: List[Record],
            input: BitVector
        ): Attempt[DecodeResult[List[Record]]] =
          if (input.isEmpty)
            Attempt.successful(DecodeResult(prev.reverse, BitVector.empty))
          else
            Record.decoder(prev).decode(input) match {
              case Attempt.Successful(result) =>
                go(result.value :: prev, result.remainder)
              case err @ Attempt.Failure(_) =>
                err
            }

        go(Nil, bits)
      }

      override def encode(value: List[Record]) =
        Encoder.encodeSeq(Record.encoder)(value)

      override def sizeBound = SizeBound.unknown
    }
}
