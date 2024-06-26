package fit4s

import scodec.Codec
import scodec.bits.BitVector
import scodec.codecs.*

/** All records contain a 1 byte Record Header that indicates whether the Record Content
  * is a definition message, a normal data message or a compressed timestamp data message.
  * The lengths of the records vary in size depending on the number and size of fields
  * within them.
  */
sealed trait RecordHeader:

  def fold[A](
      nh: RecordHeader.NormalHeader => A,
      ch: RecordHeader.CompressedTimestampHeader => A
  ): A

  def messageType: MessageType

  def localMessageType: Int

  def isExtendedDefinitionMessage: Boolean

object RecordHeader:

  final case class NormalHeader(
      messageType: MessageType,
      messageTypeSpecific: Boolean,
      reserved: Boolean,
      localMessageType: Int
  ) extends RecordHeader:
    def fold[A](
        nh: RecordHeader.NormalHeader => A,
        ch: RecordHeader.CompressedTimestampHeader => A
    ): A = nh(this)

    def isExtendedDefinitionMessage: Boolean =
      messageType == MessageType.DefinitionMessage && messageTypeSpecific

  object NormalHeader:
    val codec: Codec[NormalHeader] = (bits(3) :: uint4L).xmap(
      { case (flags, lmt) =>
        val mt =
          if (flags.get(0)) MessageType.DefinitionMessage else MessageType.DataMessage
        NormalHeader(mt, flags.get(1), flags.get(2), lmt)
      },
      { rh =>
        val flags = BitVector.bits(
          Seq(
            rh.messageType.isDefinitionMessage,
            rh.messageTypeSpecific,
            rh.reserved
          )
        )
        (flags, rh.localMessageType)
      }
    )

  final case class CompressedTimestampHeader(
      localMessageType: Int,
      timeOffsetSeconds: Int
  ) extends RecordHeader:
    def fold[A](
        nh: RecordHeader.NormalHeader => A,
        ch: RecordHeader.CompressedTimestampHeader => A
    ): A = ch(this)

    val messageType = MessageType.DataMessage

    val isExtendedDefinitionMessage: Boolean = false

  object CompressedTimestampHeader:

    val codec: Codec[CompressedTimestampHeader] = (uint2 :: uint(5)).xmap(
      { case (lmt, ts) =>
        CompressedTimestampHeader(lmt, ts)
      },
      rh => (rh.localMessageType, rh.timeOffsetSeconds)
    )

  val codec: Codec[RecordHeader] = {
    val nc: Codec[RecordHeader] =
      NormalHeader.codec.upcast[RecordHeader].withContext("NormalHeader")
    val cc: Codec[RecordHeader] = CompressedTimestampHeader.codec
      .upcast[RecordHeader]
      .withContext("CompressedTimestampHeader")
    bits(1).consume(bv => if (bv == BitVector.one) cc else nc):
      case _: NormalHeader              => BitVector.zero
      case _: CompressedTimestampHeader => BitVector.one
  }.withContext("RecordHeader")
