package fit4s

import fit4s.decode.{DataFields, DataMessageDecoder}
import fit4s.profile.FieldValue
import fit4s.profile.messages.{EventMsg, FitMessages, Msg}
import fit4s.profile.types.*
import fit4s.util.Codecs.*

import scodec.*
import scodec.bits.{ByteOrdering, ByteVector}
import scodec.codecs.*

sealed trait FitMessage:
  def fold[A](fd: FitMessage.DefinitionMessage => A, fdd: FitMessage.DataMessage => A): A

object FitMessage:

  final case class DefinitionMessage(
      reserved: Int,
      archType: ByteOrdering,
      globalMessageNumber: Either[Int, MesgNum],
      fieldCount: Int,
      fields: List[FieldDefinition],
      profileMsg: Option[Msg],
      devFieldCount: Int,
      devFields: List[FieldDefinition]
  ) extends FitMessage:
    def fold[A](
        fd: FitMessage.DefinitionMessage => A,
        fdd: FitMessage.DataMessage => A
    ): A = fd(this)

    def dataMessageLength: Int =
      fields.map(_.sizeBytes).sum + devFields.map(_.sizeBytes).sum

    def isMesgNum(n: MesgNum): Boolean =
      globalMessageNumber.exists(_ == n)

    override def toString: String =
      s"DefinitionMessage(mesgNum=$globalMessageNumber, profileMsg=$profileMsg, fieldCount=$fieldCount/$devFieldCount)"

  object DefinitionMessage:
    private val archCodec: Codec[ByteOrdering] =
      uint8.xmap(
        n => if (n == 0) ByteOrdering.LittleEndian else ByteOrdering.BigEndian,
        bo => if (bo == ByteOrdering.LittleEndian) 0 else 1
      )

    def codec(header: RecordHeader): Codec[DefinitionMessage] =
      val fieldCodec =
        uint8 :: archCodec.flatPrepend(bo =>
          fallback(uintx(16, bo), MesgNum.codec(bo)).flatPrepend { eitherMsgNum =>
            uintx(8, bo).flatPrepend { fc =>
              val msg = eitherMsgNum.toOption.flatMap(FitMessages.findByMesgNum)
              listOfN(provide(fc), FieldDefinition.codec) :: provide(msg)
            }
          }
        )

      val devFieldCodec =
        if (header.isExtendedDefinitionMessage)
          uint8.flatPrepend { fc =>
            listOfN(provide(fc), FieldDefinition.codec).tuple
          }
        else
          provide((0, List.empty[FieldDefinition]))

      fieldCodec
        .flatConcat(_ => devFieldCodec)
        .as[DefinitionMessage]
        .withContext("DefinitionMessage")

  final case class DataMessage(definition: DefinitionMessage, raw: ByteVector)
      extends FitMessage:

    def fold[A](
        fd: FitMessage.DefinitionMessage => A,
        fdd: FitMessage.DataMessage => A
    ): A = fdd(this)

    lazy val dataFields: DataFields = DataMessageDecoder.decode(this)

    lazy val isKnownMessage: Boolean =
      definition.profileMsg.isDefined

    def isMessage(m: Msg): Boolean =
      definition.profileMsg.contains(m)

    def getField[A <: TypedValue[?]](
        ft: Msg.FieldWithCodec[A]
    ): Either[String, Option[FieldValue[A]]] =
      dataFields.getDecodedField[A](ft)

    def getRequiredField[A <: TypedValue[?]](
        ft: Msg.FieldWithCodec[A]
    ): Either[String, FieldValue[A]] =
      getField(ft).flatMap:
        case Some(v) => Right(v)
        case None    =>
          Left(s"Field '${ft.fieldName}' not found in msg '${definition.profileMsg}'.")

    def unsafeGetField[A <: TypedValue[?]](
        ft: Msg.FieldWithCodec[A]
    ): Option[FieldValue[A]] =
      getField(ft).fold(sys.error, identity)

    def unsafeGetRequiredField[A <: TypedValue[?]](
        ft: Msg.FieldWithCodec[A]
    ): FieldValue[A] =
      getRequiredField(ft).fold(sys.error, identity)

    def isEvent(event: Event, eventType: EventType): Boolean =
      definition.profileMsg.contains(EventMsg) &&
        getField(EventMsg.eventType).map(_.map(_.value)).contains(Some(eventType)) &&
        getField(EventMsg.event).map(_.map(_.value)).contains(Some(event))

  object DataMessage:
    def decoder(
        prev: Map[Int, FitMessage.DefinitionMessage],
        header: RecordHeader
    ): Decoder[DataMessage] =
      lastDefinitionMessage(prev, header).flatMap(dm => decodeDataMessage(header, dm))

    // TODO compressed timestamp header: pass offset to data message
    @annotation.nowarn
    private def decodeDataMessage(
        header: RecordHeader,
        dm: DefinitionMessage
    ): Decoder[DataMessage] =
      bytes(dm.dataMessageLength).flatMap(bv => Decoder.pure(DataMessage(dm, bv)))

    private def lastDefinitionMessage(
        prev: Map[Int, FitMessage.DefinitionMessage],
        header: RecordHeader
    ): Decoder[DefinitionMessage] =
      val defMsg = prev.get(header.localMessageType)

      Decoder
        .pure(defMsg)
        .flatMap:
          case Some(v) => Decoder.pure(v)
          case None    =>
            fail(
              Err(
                s"No definition message for $header. Looked in ${prev.size} previous records: $prev"
              )
            )

    val encoder: Encoder[DataMessage] =
      Encoder(dm => Attempt.successful(dm.raw.bits))

  def encoder(header: RecordHeader): Encoder[FitMessage] =
    Encoder[FitMessage] { (m: FitMessage) =>
      m match
        case dm: DefinitionMessage => DefinitionMessage.codec(header).encode(dm)
        case dm: DataMessage       => DataMessage.encoder.encode(dm)
    }

  def decoder(
      prev: Map[Int, FitMessage.DefinitionMessage]
  )(rh: RecordHeader): Decoder[FitMessage] =
    rh.messageType match
      case MessageType.DefinitionMessage =>
        DefinitionMessage.codec(rh).upcast[FitMessage]
      case MessageType.DataMessage =>
        DataMessage.decoder(prev, rh)
