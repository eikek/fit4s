package fit4s.cli

import fit4s._
import fit4s.data.Nel
import fit4s.profile.FieldValue
import fit4s.profile.types.{FloatTypedValue, _}
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.{Codec, Decoder, Encoder, Json}
import scodec.Attempt
import scodec.bits.ByteOrdering

trait JsonEncoder {
  implicit val messageTypeEncoder: Encoder[MessageType] =
    Encoder.encodeString.contramap(_.toString)

  implicit val byteOrderingCodec: Codec[ByteOrdering] =
    Codec.from(
      Decoder.decodeString.map(s =>
        if (s.equalsIgnoreCase("little_endian")) ByteOrdering.LittleEndian
        else ByteOrdering.BigEndian
      ),
      Encoder.encodeString.contramap(_.toString)
    )

  implicit val fieldDefinitionBaseTypeCodec: Encoder[FieldDefinition.BaseType] =
    Encoder.encodeString.contramap(_.fitBaseType.typeName)

  implicit val fieldDefinitionCodec: Encoder[FieldDefinition] =
    deriveEncoder[FieldDefinition]

  implicit val mesgNumEncoder: Encoder[MesgNum] =
    Encoder.encodeString.contramap(_.toString)

  implicit val mesgNumOrNumberEncoder: Encoder[Either[Int, MesgNum]] =
    Encoder.instance {
      case Right(m) => m.asJson
      case Left(n)  => n.asJson
    }

  implicit val definitionMessageEncoder: Encoder[FitMessage.DefinitionMessage] =
    Encoder.forProduct4("architecture", "message", "fieldCount", "fields")(m =>
      (
        m.archType,
        m.globalMessageNumber,
        m.fieldCount,
        m.fields
      )
    )

  implicit val fieldValueEncoder: Encoder[FieldValue[TypedValue[_]]] =
    Encoder.instance { fval =>
      val amount = fval.scaledValue
        .map {
          case Nel(h, Nil) => h.asJson
          case l           => l.toList.asJson
        }
        .getOrElse(fval.value match {
          case LongTypedValue(rv, _)          => rv.asJson
          case ArrayFieldType.LongArray(nel)  => nel.toList.asJson
          case FloatTypedValue(rv, _)         => rv.asJson
          case ArrayFieldType.FloatArray(nel) => nel.toList.asJson
          case StringTypedValue(rv)           => rv.asJson
          case ArrayFieldType.StringArray(rv) => rv.toList.asJson
          case dt: DateTime                   => dt.asInstant.toString.asJson
          case dt: LocalDateTime              => dt.asLocalDateTime.toString.asJson
          case _                              => fval.value.toString.asJson
        })
      fval.field.unit match {
        case Some(u) => Json.obj("value" -> amount, "unit" -> u.name.asJson)
        case None    => amount
      }
    }

  implicit val dataMessageEncoder: Encoder[FitMessage.DataMessage] =
    Encoder.instance { dm =>
      val msg = dm.definition.globalMessageNumber.asJson
      dm.decoded match {
        case Attempt.Successful(DataDecodeResult(fields)) =>
          val pairs = fields
            .withFilter(_.isKnownSuccess)
            .map {
              case r: FieldDecodeResult.Success =>
                r.fieldValue.field.fieldName -> r.fieldValue.asJson
              case r: FieldDecodeResult.LocalSuccess =>
                r.localField.fieldDefNum.toString -> r.value.toString.asJson
              case r: FieldDecodeResult.DecodeError =>
                r.localField.fieldDefNum.toString -> r.err.messageWithContext.asJson
              case r: FieldDecodeResult.InvalidValue =>
                r.localField.fieldDefNum.toString -> "Invalid value".asJson
              case r: FieldDecodeResult.NoReferenceSubfield =>
                r.globalField.fieldName -> "No subfield reference".asJson
            }
          Json.obj("message" -> msg, "fields" -> Json.obj(pairs: _*))
        case Attempt.Failure(err) =>
          Json.obj("error" -> err.messageWithContext.asJson, "message" -> msg)
      }
    }

  implicit val normalHeaderEncoder: Encoder[RecordHeader.NormalHeader] =
    Encoder.forProduct2("messageType", "localMessageNum")(h =>
      (h.messageType, h.localMessageType)
    )

  implicit val compressedHeaderEncoder: Encoder[RecordHeader.CompressedTimestampHeader] =
    Encoder.forProduct3("messageType", "localMessageNum", "offset")(h =>
      (h.messageType.widen, h.localMessageType, h.timeOffsetSeconds)
    )

  implicit val recordHeaderCodec: Encoder[RecordHeader] =
    Encoder.instance {
      case h: RecordHeader.NormalHeader              => h.asJson
      case h: RecordHeader.CompressedTimestampHeader => h.asJson
    }

  implicit val recordCodec: Encoder[Record] =
    deriveEncoder[Record]

  implicit val fileHeaderCodec: Codec[FileHeader] =
    deriveCodec[FileHeader]

  implicit val fitMessageEncoder: Encoder[FitMessage] =
    Encoder.instance {
      case m: FitMessage.DefinitionMessage => m.asJson
      case m: FitMessage.DataMessage       => m.asJson
    }

  implicit val fitFileEncoder: Encoder[FitFile] =
    deriveEncoder[FitFile]
}

object JsonEncoder extends JsonEncoder
