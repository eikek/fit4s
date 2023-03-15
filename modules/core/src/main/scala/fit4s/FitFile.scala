package fit4s

import fit4s.profile.basetypes.MesgNum
import scodec.{Attempt, Codec, DecodeResult, Encoder, SizeBound}
import scodec.bits.BitVector
import scodec.codecs._

final case class FitFile(
    header: FileHeader,
    records: List[Record],
    crc: Int
) {

  def findData(n: MesgNum): Option[FitMessage.DataMessage] =
    records.collectFirst {
      case Record.DataRecord(_, cnt) if cnt.definition.isMesgNum(n) =>
        cnt
    }
}

object FitFile {

  def codec: Codec[FitFile] =
    FileHeader.codec
      .flatPrepend { header =>
        fixedSizeBytes(header.dataSize, recordsCodec) :: uint16L
      }
      .as[FitFile]

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
