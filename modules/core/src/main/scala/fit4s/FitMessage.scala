package fit4s

import scodec._
import scodec.codecs._
import scodec.bits.{ByteOrdering, ByteVector}
import fit4s.codecs._

sealed trait FitMessage

object FitMessage {

  final case class DefinitionMessage(
      reserved: Int,
      archType: ByteOrdering,
      globalMessageNumber: Int,
      fieldCount: Int,
      fields: List[FieldDefinition]
  ) extends FitMessage {
    def dataMessageLength: Int = fields.map(_.sizeBytes).sum
  }

  object DefinitionMessage {
    private val archCodec: Codec[ByteOrdering] =
      uint8.xmap(
        n => if (n == 0) ByteOrdering.LittleEndian else ByteOrdering.BigEndian,
        bo => if (bo == ByteOrdering.LittleEndian) 0 else 1
      )

    def codec: Codec[DefinitionMessage] =
      (uint8 :: archCodec.flatPrepend(bo =>
        uintx(16, bo) :: uintx(8, bo).flatPrepend(fc =>
          listOfN(provide(fc), FieldDefinition.codec).hlist
        )
      )).as[DefinitionMessage]
  }

  final case class DataMessage(raw: ByteVector) extends FitMessage {
    def lengthBytes = raw.length
  }

  object DataMessage {
    def decoder(prev: List[Record], header: RecordHeader): Decoder[DataMessage] =
      lastDefinitionMessage(prev, header).flatMap(dm =>
        bytes(dm.dataMessageLength).xmap[DataMessage](DataMessage.apply, _.raw)
      )

    private def lastDefinitionMessage(
        prev: List[Record],
        header: RecordHeader
    ): Decoder[DefinitionMessage] = {
      val defMsg = prev
        .find(r =>
          r.header.messageType.isDefinitionMessage && r.header.localMessageType == header.localMessageType
        )
        .map(_.content.asInstanceOf[FitMessage.DefinitionMessage])

      Decoder.point(defMsg).flatMap {
        case Some(v) => Decoder.point(v)
        case None =>
          val allDm = prev.filter(_.header.messageType.isDefinitionMessage)
          fail(
            Err(
              s"No definition message for $header. Looked in ${allDm.size} previous records: $allDm"
            )
          )
      }
    }

    val encoder: Encoder[DataMessage] =
      Encoder(dm => Attempt.successful(dm.raw.bits))
  }

  val encoder: Encoder[FitMessage] =
    Encoder[FitMessage] { (m: FitMessage) =>
      m match {
        case dm: DefinitionMessage => DefinitionMessage.codec.encode(dm)
        case dm: DataMessage       => DataMessage.encoder.encode(dm)
      }
    }

  def decoder(prev: List[Record])(rh: RecordHeader): Decoder[FitMessage] =
    rh.messageType match {
      case MessageType.DefinitionMessage =>
        DefinitionMessage.codec.upcast[FitMessage]
      case MessageType.DataMessage =>
        DataMessage.decoder(prev, rh)
    }
}
