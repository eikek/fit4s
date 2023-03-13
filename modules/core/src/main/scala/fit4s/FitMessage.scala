package fit4s

import scodec._
import scodec.codecs._
import scodec.bits.{ByteOrdering, ByteVector}
import fit4s.codecs._

sealed trait FitMessage {
  def lengthBytes: Long
}

object FitMessage {

  final case class DefinitionMessage(
      reserved: Int,
      archType: ByteOrdering,
      globalMessageNumber: Int,
      fieldCount: Int,
      fields: List[FieldDefinition]
  ) extends FitMessage {
    def totalLengthBytes: Int = fields.map(_.sizeBytes).sum
    def lengthBytes = 0
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
    def decoder(prev: List[Record], header: RecordHeader): Decoder[DataMessage] = {
      val allDm = prev.filter(_.header.messageType.isDefinitionMessage)
      val dm = prev
        .find(r =>
          r.header.messageType.isDefinitionMessage && r.header.localMessageType == header.localMessageType
        )
        .map(_.content.asInstanceOf[FitMessage.DefinitionMessage])
        .getOrElse(
          sys.error(
            s"no definition message for $header. Looked in ${allDm.size} previous records: $allDm"
          )
        )
      bytes(dm.totalLengthBytes).xmap[DataMessage](DataMessage.apply, _.raw)
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
