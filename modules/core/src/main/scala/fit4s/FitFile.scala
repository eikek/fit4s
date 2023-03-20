package fit4s

import fit4s.profile.types.{Event, EventType, MesgNum}
import scodec.{Attempt, Codec, DecodeResult, Encoder, SizeBound}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

final case class FitFile(
    header: FileHeader,
    records: Vector[Record],
    crc: Int
) {

  def findData(
      n: MesgNum,
      filter: FitMessage.DataMessage => Boolean = _ => true
  ): Vector[FitMessage.DataMessage] =
    records.collect {
      case Record.DataRecord(_, cnt) if cnt.definition.isMesgNum(n) && filter(cnt) =>
        cnt
    }

  def findFirstData(
      n: MesgNum,
      filter: FitMessage.DataMessage => Boolean = _ => true
  ): Either[String, FitMessage.DataMessage] =
    findData(n, filter).headOption.toRight(s"No $n message found!")

  def findEvent(
      eventType: EventType,
      event: Event
  ): Either[String, FitMessage.DataMessage] =
    findFirstData(
      MesgNum.Event,
      _.isEvent(event, eventType)
    )

  def dataRecords: Vector[FitMessage.DataMessage] =
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
  private def recordsCodec: Codec[Vector[Record]] =
    new Codec[Vector[Record]] {
      override def decode(bits: BitVector): Attempt[DecodeResult[Vector[Record]]] = {
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

        go(Nil, bits).map(_.map(_.toVector))
      }

      override def encode(value: Vector[Record]) =
        Encoder.encodeSeq(Record.encoder)(value)

      override def sizeBound = SizeBound.unknown
    }
}
