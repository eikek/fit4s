package fit4s

import fit4s.FieldDefinition.BaseType
import fit4s.profile.{FitMessages, Msg}
import fit4s.profile.basetypes.{FitBaseType, MesgNum}
import io.circe._
import io.circe.generic.semiauto._
import scodec.bits.ByteOrdering

trait JsonCodec {

  implicit val byteOrderCodec: Codec[ByteOrdering] =
    Codec.from(
      Decoder.decodeString.map(n =>
        if (n.equalsIgnoreCase("big_endian")) ByteOrdering.BigEndian
        else ByteOrdering.LittleEndian
      ),
      Encoder.encodeString.contramap(_.toJava.toString)
    )

  implicit val mesgNumCodec: Codec[MesgNum] =
    Codec.from(
      Decoder.decodeLong.emap(n => MesgNum.byRawValue(n).toRight(s"Invalid MesgNum: $n")),
      Encoder.encodeLong.contramap(_.rawValue)
    )

  implicit val fitBaseTypeCodec: Codec[FitBaseType] =
    Codec.from(
      Decoder.decodeLong.emap(n =>
        FitBaseType.byRawValue(n).toRight(s"Invalid FitBaseType: $n")
      ),
      Encoder.encodeLong.contramap(_.rawValue)
    )

  implicit val fieldDefBaseRawCodec: Codec[FieldDefinition.BaseType.Raw] =
    deriveCodec[FieldDefinition.BaseType.Raw]

  implicit val baseTypeCodec: Codec[BaseType] =
    deriveCodec[BaseType]

  implicit val fieldDefinitionCodec: Codec[FieldDefinition] =
    deriveCodec[FieldDefinition]

  implicit val profileMsgCodec: Codec[Msg] =
    mesgNumCodec.iemap(n =>
      FitMessages.findByMesgNum(n).toRight(s"No profile message found for mesgNum: $n")
    )(_.globalMessageNumber)

  implicit val eitherIntMesgNumCodec: Codec[Either[Int, MesgNum]] =
    Codec.from(
      Decoder.decodeLong.map(n => MesgNum.byRawValue(n).toRight(n.toInt)),
      Encoder.encodeLong.contramap(_.fold(_.toLong, _.rawValue))
    )

  implicit val fieldDefinitionMsgCodec: Codec[FitMessage.DefinitionMessage] =
    deriveCodec[FitMessage.DefinitionMessage]
}
