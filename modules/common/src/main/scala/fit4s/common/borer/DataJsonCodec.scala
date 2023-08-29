package fit4s.common.borer

import java.time.{Duration, Instant, ZoneId}

import cats.Show
import cats.syntax.all.*

import fit4s.cats.syntax.all.*
import fit4s.cats.util.DateInstant
import fit4s.common.borer.CoreJsonCodec
import fit4s.common.borer.syntax.all.*
import fit4s.data.*
import fit4s.profile.types.Sport

import io.bullet.borer.NullOptions.*
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

trait DataJsonCodec extends CoreJsonCodec {
  implicit def deviceProductCodec: Codec[DeviceProduct] =
    Codec.of(
      Encoder.forString.contramap(_.name),
      Decoder.forString.emap(DeviceProduct.fromString)
    )

  // implicit def fileIdCodec: Codec[FileId] =
  //   Codec.of(
  //     Encoder.forString.contramap(_.asString),
  //     Decoder.forString.emap(FileId.fromString)
  //   )
  implicit val fileIdEncoder: Encoder[FileId] =
    deriveEncoder

  implicit val fileIdDecoder: Decoder[FileId] =
    deriveDecoder

  implicit val deviceProductEncoder: Encoder[DeviceProduct] =
    Encoder.forString.contramap(_.name)

  implicit val durationEncoder: Encoder[Duration] =
    valueEncoder("millis", _.toMillis)

  implicit val durationDecoder: Decoder[Duration] =
    valueDecoder[Long]("millis").map(Duration.ofMillis)

  implicit val semicircleEncoder: Encoder[Semicircle] =
    valueEncoder("semicircle", _.semicircle)

  implicit val semicircleDecoder: Decoder[Semicircle] =
    valueDecoder[Long]("semicircle").map(Semicircle.semicircle)

  implicit val distanceEncoder: Encoder[Distance] =
    valueEncoder("meter", _.meter)

  implicit val distanceDecoder: Decoder[Distance] =
    valueDecoder[Double]("meter").map(Distance.meter)

  implicit val positionEncoder: Encoder[Position] =
    deriveEncoder

  implicit val positionDecoder: Decoder[Position] =
    deriveDecoder

  implicit val caloriesEncoder: Encoder[Calories] =
    valueEncoder("kcal", _.kcal)

  implicit val caloriesDecoder: Decoder[Calories] =
    valueDecoder[Double]("kcal").map(Calories.kcal)

  implicit val temperatureEncoder: Encoder[Temperature] =
    valueEncoder("celcius", _.celcius)

  implicit val temperatureDecoder: Decoder[Temperature] =
    valueDecoder[Double]("celcius").map(Temperature.celcius)

  implicit val heartRateEncoder: Encoder[HeartRate] =
    valueEncoder("bpm", _.bpm)

  implicit val heartRateDecoder: Decoder[HeartRate] =
    valueDecoder[Int]("bpm").map(HeartRate.bpm)

  implicit def speedEncoder(implicit sport: Sport): Encoder[Speed] =
    valueEncoder("meterPerSecond", _.meterPerSecond)

  implicit val speedDecoder: Decoder[Speed] =
    valueDecoder[Double]("meterPerSecond").map(Speed.meterPerSecond)

  implicit val powerEncoder: Encoder[Power] =
    valueEncoder("watt", _.watts)

  implicit val powerDecoder: Decoder[Power] =
    valueDecoder[Int]("watt").map(Power.watts)

  implicit val cadenceEncoder: Encoder[Cadence] =
    valueEncoder("rpm", _.rpm)

  implicit val cadenceDecoder: Decoder[Cadence] =
    valueDecoder[Int]("rpm").map(Cadence.rpm)

  implicit val trainingStressScoreEncoder: Encoder[TrainingStressScore] =
    valueEncoder("tss", _.tss)

  implicit val trainingStressScoreDecoder: Decoder[TrainingStressScore] =
    valueDecoder[Double]("tss").map(TrainingStressScore.tss)

  implicit val intensityFactorEncoder: Encoder[IntensityFactor] =
    valueEncoder("if", _.iff)

  implicit val intensityFactorDecoder: Decoder[IntensityFactor] =
    valueDecoder[Double]("if").map(IntensityFactor.iff)

  implicit val percentEncoder: Encoder[Percent] =
    valueEncoder("percent", _.percent)

  implicit val percentDecoder: Decoder[Percent] =
    valueDecoder[Double]("percent").map(Percent.percent)

  implicit val gradeEncoder: Encoder[Grade] =
    valueEncoder("grade", _.percent)

  implicit val gradeDecoder: Decoder[Grade] =
    valueDecoder[Double]("grade").map(Grade.percent)

  implicit val strokesPerLapEncoder: Encoder[StrokesPerLap] =
    valueEncoder("strokesPerLap", _.spl)

  implicit val strokesPerLapDecoder: Decoder[StrokesPerLap] =
    valueDecoder[Double]("strokesPerLap").map(StrokesPerLap.strokesPerLap)

  implicit def dateInstantEncoder(implicit zoneId: ZoneId): Encoder[DateInstant] =
    valueEncoder("timestamp", _.instant)

  implicit val dateInstantDecoder: Decoder[DateInstant] =
    valueDecoder[Instant]("timestamp").map(DateInstant.apply)
}

object DataJsonCodec extends DataJsonCodec
