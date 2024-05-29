package fit4s

import fit4s.FitMessage.DataMessage
import fit4s.data.FileId
import fit4s.profile.types.MesgNum
import fit4s.util.Nel

import scodec.*
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.*

final case class FitFile(
    header: FileHeader,
    records: Vector[Record],
    crc: Int
):

  def filterRecords(f: Record => Boolean): FitFile =
    copy(records = records.filter(f))

  def filterDataRecords(f: DataMessage => Boolean): FitFile =
    filterRecords:
      case Record.DataRecord(_, r) => f(r)
      case _                       => true

  def findData(
      n: MesgNum,
      filter: FitMessage.DataMessage => Boolean = _ => true
  ): Vector[FitMessage.DataMessage] =
    records.collect:
      case Record.DataRecord(_, cnt) if cnt.definition.isMesgNum(n) && filter(cnt) =>
        cnt

  def findFirstData(
      n: MesgNum,
      filter: FitMessage.DataMessage => Boolean = _ => true
  ): Either[String, FitMessage.DataMessage] =
    findData(n, filter).headOption.toRight(s"No $n message found!")

  def findFileId: Either[String, FileId] =
    findFirstData(MesgNum.FileId).flatMap(FileId.from)

  def dataRecords: Vector[FitMessage.DataMessage] =
    records.collect { case Record.DataRecord(_, cnt) =>
      cnt
    }

object FitFile:

  def singleCodec: Codec[FitFile] =
    FileHeader.codec
      .flatPrepend { header =>
        fixedSizeBytes(header.dataSize, recordsCodec.withContext("Record")) :: uint16L
      }
      .as[FitFile]

  def codec: Codec[List[FitFile]] =
    list(singleCodec)

  /** Decodes from all input bytes and expects at least on fit file. */
  def decode(bv: ByteVector): Attempt[Nel[FitFile]] =
    codec.complete
      .decode(bv.bits)
      .map(_.value)
      .map(Nel.fromList)
      .flatMap:
        case Some(nel) => Attempt.successful(nel)
        case None      => Attempt.failure(Err("No fit file found."))

  def decodeUnsafe(bv: ByteVector): Nel[FitFile] =
    decode(bv).fold(
      err => sys.error(s"Decoding FIT failed! ${err.messageWithContext}"),
      identity
    )

  private def recordsCodec: Codec[Vector[Record]] =
    new Codec[Vector[Record]]:
      override def decode(bits: BitVector): Attempt[DecodeResult[Vector[Record]]] =
        @annotation.tailrec
        def go(
            prev: Map[Int, FitMessage.DefinitionMessage],
            records: List[Record],
            input: BitVector
        ): Attempt[DecodeResult[List[Record]]] =
          if (input.isEmpty)
            Attempt.successful(DecodeResult(records.reverse, BitVector.empty))
          else
            Record.decoder(prev).decode(input) match
              case Attempt.Successful(result) =>
                val prevMap =
                  result.value match
                    case Record.DefinitionRecord(h, m) =>
                      prev.updated(h.localMessageType, m)
                    case _ =>
                      prev
                go(prevMap, result.value :: records, result.remainder)
              case err @ Attempt.Failure(_) =>
                err

        go(Map.empty, Nil, bits).map(_.map(_.toVector))

      override def encode(value: Vector[Record]): Attempt[BitVector] =
        val init = Attempt.successful(BitVector.empty)
        value
          .map(Record.encoder.encode)
          .foldLeft(init)((a, b) => a.flatMap(b1 => b.map(b2 => b1 ++ b2)))

      override def sizeBound = SizeBound.unknown
