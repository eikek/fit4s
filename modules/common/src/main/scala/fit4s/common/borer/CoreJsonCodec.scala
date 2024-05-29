package fit4s.common.borer

import java.time.{Duration, Instant}

import scala.util.Try

import cats.Show
import cats.data.NonEmptyList
import cats.syntax.all.*

import fit4s.common.borer.syntax.all.*
import fit4s.profile.types.*

import io.bullet.borer.*

/** Codecs for fit4s.profile */
trait CoreJsonCodec:
  implicit def stringMapEncoder[A: Encoder]: Encoder[Map[String, A]] =
    Encoder { (w, m) =>
      w.writeMapOpen(m.size)
      m.foreach { case (k, v) => w.writeMapMember(k, v) }
      w.writeMapClose()
    }

  implicit def stringMapDecoder[A: Decoder]: Decoder[Map[String, A]] =
    Decoder { reader =>
      @annotation.tailrec
      def loop(result: Map[String, A]): Map[String, A] =
        if (reader.hasString)
          val key = reader.readString()
          val v = reader.read[A]()
          loop(result.updated(key, v))
        else result

      reader.readMapStart()
      val x = loop(Map.empty)
      reader.readMapClose(true, x)
    }

  def valueEncoder[A: Show, B: Encoder](name: String, f: A => B): Encoder[A] =
    Encoder { (writer, a) =>
      writer.writeMapStart()
      writer.writeMapMember("label", a.show)
      writer.writeMapMember(name, f(a))
      writer.writeMapClose()
    }

  def valueDecoder[B: Decoder](name: String): Decoder[B] =
    Decoder { r =>
      def readB: B =
        if (r.readString() == name)
          val b = r.read[B]()
          r.readString()
          r.readString()
          b
        else
          r.readString()
          r.readString(name)
          r.read[B]()

      r.readMapStart()
      val b = readB
      r.readMapClose(true, b)
    }

  implicit def instantCodec: Codec[Instant] =
    Codec(
      Encoder.forString.contramap(_.toString),
      Decoder.forString.emap(s => Try(Instant.parse(s)).toEither.left.map(_.getMessage))
    )

  implicit def durationCodec: Codec[Duration] =
    Codec(
      Encoder.forLong.contramap(_.toSeconds),
      Decoder.forLong.map(Duration.ofSeconds)
    )

  implicit def nonEmptyListEncoder[A: Encoder]: Encoder[NonEmptyList[A]] =
    Encoder[List[A]].contramap(_.toList)

  implicit def nonEmptyListDecoder[A: Decoder]: Decoder[NonEmptyList[A]] =
    Decoder[List[A]].emap(l =>
      NonEmptyList.fromList(l).toRight("Expected non-empty list, but list was empty")
    )

  implicit def baseTypeEncoder[A <: TypedValue[?]]: Encoder[A] =
    Encoder.forString.contramap(_.typeName)

  implicit def sportDecoder: Decoder[Sport] =
    Decoder.forString.emap(s => Sport.byTypeName(s).toRight(s"Invalid sport: $s"))

  implicit val triggerDecoder: Decoder[LapTrigger] =
    Decoder.forString.emap(s =>
      LapTrigger.byTypeName(s).toRight(s"Invalid lap trigger: $s")
    )

  implicit val manufacturerDecoder: Decoder[Manufacturer] =
    Decoder.forString.emap(s =>
      Manufacturer.byTypeName(s).toRight(s"Invalid manufacturer: $s")
    )

  implicit val fileEnumDecoder: Decoder[File] =
    Decoder.forString.emap(s => File.byTypeName(s).toRight(s"Invalid file enum: $s"))

  implicit def subSportDecoder: Decoder[SubSport] =
    Decoder.forString.emap(s => SubSport.byTypeName(s).toRight(s"Invalid sub_sport: $s"))

  implicit val swimStrokeDecoder: Decoder[SwimStroke] =
    Decoder.forString.emap(s =>
      SwimStroke.byTypeName(s).toRight(s"Invalid swim stroke: $s")
    )

  implicit val dateTimeEncoder: Encoder[DateTime] =
    Encoder.forLong.contramap(_.rawValue)

  implicit val dateTimeDecoder: Decoder[DateTime] =
    Decoder.forLong.map(DateTime(_))

object CoreJsonCodec extends CoreJsonCodec
