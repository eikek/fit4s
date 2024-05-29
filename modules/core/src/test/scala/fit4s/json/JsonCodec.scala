package fit4s.json

import fit4s.FieldDefinition.BaseType
import fit4s.profile.messages.{FitMessages, Msg}
import fit4s.profile.types.{FitBaseType, MesgNum}
import fit4s.{FieldDefinition, FitMessage}

import io.bullet.borer.NullOptions.given
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import scodec.bits.ByteOrdering

trait JsonCodec:
  extension [A](delegate: Decoder[A])
    def emap[B](f: A => Either[String, B]): Decoder[B] =
      delegate.mapWithReader((r, a) => f(a).fold(r.validationFailure, identity))

  implicit val byteOrderCodec: Codec[ByteOrdering] =
    Codec(
      Encoder.forString.contramap(_.toJava.toString),
      Decoder.forString.map(n =>
        if (n.equalsIgnoreCase("big_endian")) ByteOrdering.BigEndian
        else ByteOrdering.LittleEndian
      )
    )

  implicit val mesgNumCodec: Codec[MesgNum] =
    Codec(
      Encoder.forLong.contramap(_.rawValue),
      Decoder.forLong.emap(n => MesgNum.byRawValue(n).toRight(s"Invalid MesgNum: $n"))
    )

  implicit val fitBaseTypeCodec: Codec[FitBaseType] =
    Codec(
      Encoder.forLong.contramap(_.rawValue),
      Decoder.forLong.emap(n =>
        FitBaseType.byRawValue(n).toRight(s"Invalid FitBaseType: $n")
      )
    )

  implicit val fieldDefBaseRawCodec: Codec[FieldDefinition.BaseType.Raw] =
    deriveCodec[FieldDefinition.BaseType.Raw]

  implicit val baseTypeCodec: Codec[BaseType] =
    deriveCodec[BaseType]

  implicit val fieldDefinitionCodec: Codec[FieldDefinition] =
    deriveCodec[FieldDefinition]

  implicit val profileMsgCodec: Codec[Msg] =
    mesgNumCodec.bimap(
      _.globalMessageNumber,
      n =>
        FitMessages
          .findByMesgNum(n)
          .getOrElse(sys.error(s"No profile message found for mesgNum: $n"))
    )

  implicit val eitherIntMesgNumCodec: Codec[Either[Int, MesgNum]] =
    Codec(
      Encoder.forLong.contramap(_.fold(_.toLong, _.rawValue)),
      Decoder.forLong.map(n => MesgNum.byRawValue(n).toRight(n.toInt))
    )

  implicit val fieldDefinitionMsgCodec: Codec[FitMessage.DefinitionMessage] =
    deriveCodec[FitMessage.DefinitionMessage]
