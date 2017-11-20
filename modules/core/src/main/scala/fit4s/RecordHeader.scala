package fit4s

import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs._

import RecordHeader._

case class RecordHeader(headerType: HeaderType
  , messageType: MessageType
  , messageTypeSpecific: Boolean
  , reserved: Boolean
  , localMessageType: Int)


object RecordHeader {

  sealed trait HeaderType
  object HeaderType {
    case object Normal extends HeaderType
    case object CompressedTimestamp extends HeaderType
  }

  sealed trait MessageType
  object MessageType {
    case object DefinitionMessage extends MessageType
    case object DataMessage extends MessageType
  }

  val codec: Codec[RecordHeader] = (bits(4) ~ uint4L).xmap(
    { case (flags, lmt) =>
      val ht = if (flags.get(0)) HeaderType.Normal else HeaderType.CompressedTimestamp
      val mt = if (flags.get(1)) MessageType.DefinitionMessage else MessageType.DataMessage
      RecordHeader(ht, mt, flags.get(2), flags.get(3), lmt)
    },
    { rh =>
      val flags = BitVector.bits(Seq(rh.headerType == HeaderType.Normal
        , rh.messageType == MessageType.DefinitionMessage
        , rh.messageTypeSpecific
        , rh.reserved))
      (flags, rh.localMessageType)
    }
  )
}
