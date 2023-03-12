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
  ) extends FitMessage

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

  final case class DataMessage(raw: ByteVector) extends FitMessage

  object DataMessage {
    def codec: Codec[DataMessage] = ???
  }

  def codec(rh: RecordHeader): Codec[FitMessage] =
    rh.messageType match {
      case MessageType.DefinitionMessage =>
        DefinitionMessage.codec.upcast[FitMessage]
      case MessageType.DataMessage =>
        DataMessage.codec.upcast[FitMessage]
    }
}
