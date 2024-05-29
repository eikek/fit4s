package fit4s.cli

import fit4s.*
import fit4s.profile.FieldValue
import fit4s.profile.types.*
import fit4s.profile.types.BaseTypedValue.{FloatBaseValue, LongBaseValue, StringBaseValue}
import fit4s.util.Nel

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.{deriveCodec, deriveEncoder}
import scodec.bits.ByteOrdering

trait JsonEncoder {
  implicit val messageTypeEncoder: Encoder[MessageType] =
    Encoder.forString.contramap(_.toString)

  implicit val byteOrderingCodec: Codec[ByteOrdering] =
    Codec(
      Encoder.forString.contramap(_.toString),
      Decoder.forString.map(s =>
        if (s.equalsIgnoreCase("little_endian")) ByteOrdering.LittleEndian
        else ByteOrdering.BigEndian
      )
    )

  implicit val fieldDefinitionBaseTypeCodec: Encoder[FieldDefinition.BaseType] =
    Encoder.forString.contramap(_.fitBaseType.typeName)

  implicit val fieldDefinitionCodec: Encoder[FieldDefinition] =
    deriveEncoder[FieldDefinition]

  implicit val mesgNumEncoder: Encoder[MesgNum] =
    Encoder.forString.contramap(_.toString)

  implicit val mesgNumOrNumberEncoder: Encoder[Either[Int, MesgNum]] =
    Encoder { (w, r) =>
      r.fold(w.write(_), w.write(_))
    }

  implicit val definitionMessageEncoder: Encoder[FitMessage.DefinitionMessage] =
    Encoder { (w, m) =>
      w.writeMapStart()
      w.writeMapMember("architecture", m.archType)
      w.writeMapMember("message", m.globalMessageNumber)
      w.writeMapMember("fieldCount", m.fieldCount)
      w.writeMapMember("fields", m.fields)
      w.writeMapClose()
    }

  implicit val fieldValueEncoder: Encoder[FieldValue[TypedValue[?]]] =
    Encoder { (w, fval) =>
      def writeAmount = fval.scaledValue
        .map {
          case Nel(h, Nil) => w.write(h)
          case l           => w.write(l.toList)
        }
        .getOrElse(fval.value match {
          case LongBaseValue(rv, _)           => w.write(rv)
          case ArrayFieldType.LongArray(nel)  => w.write(nel.toList)
          case FloatBaseValue(rv, _)          => w.write(rv)
          case ArrayFieldType.FloatArray(nel) => w.write(nel.toList)
          case StringBaseValue(rv)            => w.write(rv)
          case ArrayFieldType.StringArray(rv) => w.write(rv.toList)
          case ArrayFieldType(nel, _)         => w.write(nel.toList.map(_.toString))
          case dt: DateTime                   => w.write(dt.asInstant.toString)
          case dt: LocalDateTime              => w.write(dt.asLocalDateTime.toString)
          case _                              => w.write(fval.value.toString)
        })
      fval.field.unit match {
        case Some(u) =>
          w.writeMapStart()
          w.write("value")
          writeAmount
          w.writeMapMember("unit", u.name)
          w.writeMapClose()
        case None =>
          writeAmount
      }
    }

  implicit val dataMessageEncoder: Encoder[FitMessage.DataMessage] =
    Encoder { (w, dm) =>
      val pairs = dm.dataFields.allFields
        .flatMap(_.decodedValue.toOption)
        .flatMap(_.asSuccess) // only interested in known data
        .map { r =>
          r.fieldValue.field.fieldName -> r.fieldValue
        }

      w.writeMapStart()
      w.writeMapMember("message", dm.definition.globalMessageNumber)
      w.write("fields")
      w.writeMapStart()
      pairs.foreach { case (f, v) =>
        w.writeMapMember(f, v)
      }
      w.writeMapClose()
    }

  implicit val normalHeaderEncoder: Encoder[RecordHeader.NormalHeader] =
    Encoder { (w, h) =>
      w.writeMapStart()
      w.writeMapMember("messageType", h.messageType)
      w.writeMapMember("localMessageNum", h.localMessageType)
      w.writeMapClose()
    }

  implicit val compressedHeaderEncoder: Encoder[RecordHeader.CompressedTimestampHeader] =
    Encoder { (w, h) =>
      w.writeMapStart()
      w.writeMapMember("messageType", h.messageType.widen)
      w.writeMapMember("localMessageNum", h.localMessageType)
      w.writeMapMember("offset", h.timeOffsetSeconds)
      w.writeMapClose()
    }

  implicit val recordHeaderCodec: Encoder[RecordHeader] =
    Encoder { (w, h) =>
      h.fold(w.write(_), w.write(_))
    }

  implicit val recordCodec: Encoder[Record] =
    deriveEncoder[Record]

  implicit val fileHeaderCodec: Codec[FileHeader] =
    deriveCodec[FileHeader]

  implicit val fitMessageEncoder: Encoder[FitMessage] =
    Encoder { (w, fm) =>
      fm.fold(w.write(_), w.write(_))
    }

  implicit val fitFileEncoder: Encoder[FitFile] =
    deriveEncoder[FitFile]
}

object JsonEncoder extends JsonEncoder
