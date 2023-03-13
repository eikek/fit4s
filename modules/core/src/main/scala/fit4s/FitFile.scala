package fit4s

import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, SizeBound}
import scodec.bits.BitVector
import scodec.codecs._

final case class FitFile(
    header: FileHeader,
    records: List[Record],
    crc: Int
)

object FitFile {

  def codec: Codec[FitFile] =
    FileHeader.codec
      .flatPrepend { header =>
        println(s"got header: $header")
        fixedSizeBytes(header.dataSize, recordsCodec) :: uint16L
      }
      .as[FitFile]

  private def recordsCodec: Codec[List[Record]] =
    new Codec[List[Record]] {
      override def decode(bits: BitVector): Attempt[DecodeResult[List[Record]]] = {
        val first: Decoder[Record] =
          Record.dmCodec.withContext("First definition message")

        def remain(prev: List[Record]): Decoder[List[Record]] =
          (bits: BitVector) =>
            if (bits.isEmpty) {
              println(s"input empty. decoded ${prev.size} records")
              Attempt.successful(DecodeResult(prev.reverse, bits))
            } else {
              Record.decoder(prev).decode(bits) match {
                case Attempt.Successful(result) =>
                  println(s"got new record: $result")
                  remain(result.value :: prev).decode(result.remainder)
                case err @ Attempt.Failure(_) => err
              }
            }

        first
          .flatMap { fst =>
            println(
              s"got fst record: $fst (${fst.content.asInstanceOf[FitMessage.DefinitionMessage].totalLengthBytes})"
            )
            remain(List(fst))
          }
          .decode(bits)
      }

      override def encode(value: List[Record]) =
        Encoder.encodeSeq(Record.encoder)(value)

      override def sizeBound = SizeBound.unknown
    }
}
