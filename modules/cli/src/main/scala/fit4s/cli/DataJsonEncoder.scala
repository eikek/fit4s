package fit4s.cli

import cats.Show
import cats.syntax.all._
import fit4s.activities.data._
import fit4s.cli.FormatDefinition._
import fit4s.data._
import fit4s.profile.types._
import fs2.io.file.Path
import io.circe._
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._

import java.time.{Duration, ZoneId}

trait DataJsonEncoder {
  def typedValueEncoder[A <: TypedValue[_]]: Encoder[A] =
    Encoder.encodeString.contramap(_.typeName)

  def valueEncoder[A: Show, B: Encoder](name: String, f: A => B): Encoder[A] =
    Encoder.instance { a =>
      Json.obj(
        "label" -> a.show.asJson,
        name -> f(a).asJson
      )
    }

  implicit val dateTimeEncoder: Encoder[DateTime] =
    Encoder.encodeInstant.contramap(_.asInstant)

  implicit val deviceProductEncoder: Encoder[DeviceProduct] =
    Encoder.encodeString.contramap(_.name)

  implicit val activityIdEncoder: Encoder[ActivityId] =
    Encoder.encodeLong.contramap(_.id)

  implicit val pathEncoder: Encoder[Path] =
    Encoder.encodeString.contramap(_.absolute.toString)

  implicit val manufacturerEncoder: Encoder[Manufacturer] =
    typedValueEncoder

  implicit val fileEnumEncoder: Encoder[File] =
    typedValueEncoder

  implicit val fileIdEncoder: Encoder[FileId] =
    deriveEncoder[FileId]

  implicit val durationEncoder: Encoder[Duration] =
    valueEncoder("millis", _.toMillis)

  implicit val sportEncoder: Encoder[Sport] =
    typedValueEncoder

  implicit val subSportEncoder: Encoder[SubSport] =
    typedValueEncoder

  implicit val semicirceEncoder: Encoder[Semicircle] =
    Encoder.encodeLong.contramap(_.semicircle)

  implicit val distanceEncoder: Encoder[Distance] =
    valueEncoder("meter", _.meter)

  implicit val positionEncoder: Encoder[Position] =
    deriveEncoder

  implicit val caloriesEncoder: Encoder[Calories] =
    valueEncoder("kcal", _.kcal)

  implicit val temperatureEncoder: Encoder[Temperature] =
    valueEncoder("celcius", _.celcius)

  implicit val heartRateEncoder: Encoder[HeartRate] =
    valueEncoder("bpm", _.bpm)

  implicit def speedEncoder(implicit sport: Sport): Encoder[Speed] =
    valueEncoder("meterPerSecond", _.meterPerSecond)

  implicit val powerEncoder: Encoder[Power] =
    valueEncoder("watt", _.watts)

  implicit val cadenceEncoder: Encoder[Cadence] =
    valueEncoder("rpm", _.rpm)

  implicit val trainingStressScoreEncoder: Encoder[TrainingStressScore] =
    valueEncoder("tss", _.tss)

  implicit val intensityFactorEncoder: Encoder[IntensityFactor] =
    valueEncoder("if", _.iff)

  implicit val swimStrokeEncoder: Encoder[SwimStroke] =
    typedValueEncoder

  implicit val percentEncoder: Encoder[Percent] =
    valueEncoder("percent", _.percent)

  implicit val strokesPerLapEncoder: Encoder[StrokesPerLap] =
    valueEncoder("strokesPerLap", _.spl)

  implicit def dateInstantEncoder(implicit zoneId: ZoneId): Encoder[DateInstant] =
    valueEncoder("timestamp", _.instant)
}

object DataJsonEncoder extends DataJsonEncoder
