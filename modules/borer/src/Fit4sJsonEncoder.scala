package fit4s.borer

import fit4s.codec.*
import fit4s.core.*
import fit4s.core.data.CommonShow.given
import fit4s.core.data.DateTime
import fit4s.core.data.Display
import fit4s.profile.*

import io.bullet.borer.*
import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.deriveEncoder
import scodec.bits.ByteVector

trait Fit4sJsonEncoder:
  given Encoder[DateTime] =
    Encoder.forString.contramap(_.asInstant.toString)
  given Encoder[MeasurementUnit] =
    Encoder.forString.contramap(_.name)
  given Encoder[ByteSize] =
    new Encoder[ByteSize] {
      def write(w: Writer, value: ByteSize): Writer =
        w.writeMapOpen(2)
        w.writeMapMember("bytes", value.toBytes)
        w.writeMapMember("display", value.asString)
        w.writeMapClose()
    }

  given fileHeader: Encoder[FileHeader] =
    deriveEncoder[FileHeader]

  private def writeSingleOrVector[A: Encoder](w: Writer, key: String, vs: Vector[A]) =
    if vs.isEmpty then w.writeMapMember(key, vs)
    else if vs.tail.isEmpty then w.writeMapMember(key, vs.head)
    else w.writeMapMember(key, vs)

  given fitMessageEncoder: Encoder[FitMessage] =
    new Encoder[FitMessage] {
      def write(w: Writer, value: FitMessage): Writer =
        val all = value.allValues
        w.writeMapStart()
        value.timestamp.foreach(ts => w.writeMapMember("timestamp", ts))
        all.filter(_.fieldName != CommonMsg.timestamp.fieldName).foreach { fv =>
          w.write(fv.fieldName)
          w.writeMapStart()
          w.writeMapMember("display", Display[FieldValue].show(fv))
          fv.as[ByteVector].foreach(bv => w.writeMapMember("value", bv.toHex))
          fv.as[Vector[Int]].foreach(v => writeSingleOrVector(w, "value", v))
          fv.as[Vector[Long]].foreach(v => writeSingleOrVector(w, "value", v))
          fv.as[Vector[Double]].foreach(v => writeSingleOrVector(w, "value", v))
          fv.as[Vector[String]].foreach(v => writeSingleOrVector(w, "value", v))
          w.writeMapMember("unit", fv.unit)
          DevFieldId.fromInt(fv.fieldNumber).foreach { devId =>
            w.writeMapMember("is_dev_field", true)
            val (devIdx, devFieldNum) = devId.extracted
            w.writeMapMember("developer_data_index", devIdx)
            w.writeMapMember("developer_field_num", devFieldNum)
          }
          w.writeMapClose()
        }
        w.writeMapClose()
    }

  given fitEncoder: Encoder[Fit] =
    new Encoder[Fit] {
      def write(w: Writer, value: Fit): Writer =
        w.writeMapStart()
        w.writeMapMember("header", value.file.header)
        w.writeMapMember("crc", value.file.crc)

        w.write("messages")
        w.writeMapStart()
        for (gm <- GlobalMessages.values.values) {
          val msgName = MesgNumType.values(gm.globalNum)
          val msgs = value.getMessages(gm).toVector
          if (msgs.nonEmpty) {
            w.writeMapMember(msgName, msgs)
          }
        }
        w.writeMapClose()

        w.writeMapClose()
    }

object Fit4sJsonEncoder extends Fit4sJsonEncoder
