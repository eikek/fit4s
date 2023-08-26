package fit4s.activities.dump

import java.time.Duration
import java.time.Instant

import fit4s.activities.data.*
import fit4s.activities.records.*
import fit4s.common.borer.syntax.all.*
import fit4s.data.*
import fit4s.profile.types.LapTrigger
import fit4s.profile.types.SwimStroke
import fit4s.profile.types.{Sport, SubSport}

import io.bullet.borer.derivation.MapBasedCodecs.*
import io.bullet.borer.{Decoder, Encoder}

enum DumpFormat:
  case DTag(value: Tag)
  case DLocation(value: Location)
  case DActivity(value: Activity)
  case DSession(value: ActivitySession)
  case DLap(value: ActivityLap)
  case DSessionData(value: ActivitySessionData)
  case DActivityTag(value: RActivityTag)
  case DActivityStrava(value: RActivityStrava)
  case DGeoPlace(value: GeoPlace)
  case DActivityGeoPlace(value: RActivityGeoPlace)
  case DStravaToken(value: RStravaToken)

object DumpFormat:
  def apply(value: Tag): DumpFormat = DTag(value)
  def apply(value: Location): DumpFormat = DLocation(value)
  def apply(value: Activity): DumpFormat = DActivity(value)
  def apply(value: ActivitySession): DumpFormat = DSession(value)
  def apply(value: ActivityLap): DumpFormat = DLap(value)
  def apply(value: ActivitySessionData): DumpFormat = DSessionData(value)
  def apply(value: RActivityTag): DumpFormat = DActivityTag(value)
  def apply(value: RActivityStrava): DumpFormat = DActivityStrava(value)
  def apply(value: GeoPlace): DumpFormat = DGeoPlace(value)
  def apply(value: RActivityGeoPlace): DumpFormat = DActivityGeoPlace(value)
  def apply(value: RStravaToken): DumpFormat = DStravaToken(value)

  given Encoder[Location] = deriveEncoder
  given Decoder[Location] = deriveDecoder
  given Encoder[DLocation] = deriveEncoder
  given Decoder[DLocation] = deriveDecoder
  given Encoder[DTag] = deriveEncoder
  given Decoder[DTag] = deriveDecoder

  given Encoder[Duration] = Encoder.forLong.contramap(_.toMillis())
  given Decoder[Duration] = Decoder.forLong.map(Duration.ofMillis(_))
  given Encoder[Instant] = Encoder.forLong.contramap(_.toEpochMilli())
  given Decoder[Instant] = Decoder.forLong.map(Instant.ofEpochMilli)
  given Encoder[DeviceProduct] = Encoder.forString.contramap(_.name)
  given Decoder[DeviceProduct] = Decoder.forString.emap(DeviceProduct.fromString)
  given Encoder[DeviceInfo] = Encoder.forString.contramap(_.name)
  given Decoder[DeviceInfo] = Decoder.forString.map(DeviceInfo.fromString)
  given Encoder[FileId] = Encoder.forString.contramap(_.asString)
  given Decoder[FileId] = Decoder.forString.emap(FileId.fromString)
  given Encoder[Activity] = deriveEncoder
  given Decoder[Activity] = deriveDecoder
  given Encoder[DActivity] = deriveEncoder
  given Decoder[DActivity] = deriveDecoder

  given Encoder[Sport] = Encoder.forString.contramap(_.typeName)
  given Decoder[Sport] =
    Decoder.forString.emap(s => Sport.byTypeName(s).toRight(s"Invalid sport: $s"))
  given Encoder[SubSport] = Encoder.forString.contramap(_.typeName)
  given Decoder[SubSport] =
    Decoder.forString.emap(s => SubSport.byTypeName(s).toRight(s"Invalid sub sport: $s"))
  given Encoder[Distance] = Encoder.forDouble.contramap(_.meter)
  given Decoder[Distance] = Decoder.forDouble.map(Distance.meter)
  given Encoder[Semicircle] = Encoder.forLong.contramap(_.semicircle)
  given Decoder[Semicircle] = Decoder.forLong.map(Semicircle.semicircle)
  given Encoder[Position] =
    Encoder.forTuple[(Semicircle, Semicircle)].contramap(p => (p.latitude, p.longitude))
  given Decoder[Position] =
    Decoder.forTuple[(Semicircle, Semicircle)].map(Position.apply.tupled)
  given Encoder[Calories] = Encoder.forDouble.contramap(_.kcal)
  given Decoder[Calories] = Decoder.forDouble.map(Calories.kcal)
  given Encoder[Temperature] = Encoder.forDouble.contramap(_.celcius)
  given Decoder[Temperature] = Decoder.forDouble.map(Temperature.celcius)
  given Encoder[HeartRate] = Encoder.forInt.contramap(_.bpm)
  given Decoder[HeartRate] = Decoder.forInt.map(HeartRate.bpm)
  given Encoder[Speed] = Encoder.forDouble.contramap(_.meterPerSecond)
  given Decoder[Speed] = Decoder.forDouble.map(Speed.meterPerSecond)
  given Encoder[Power] = Encoder.forInt.contramap(_.watts)
  given Decoder[Power] = Decoder.forInt.map(Power.watts)
  given Encoder[Cadence] = Encoder.forInt.contramap(_.rpm)
  given Decoder[Cadence] = Decoder.forInt.map(Cadence.rpm)
  given Encoder[TrainingStressScore] = Encoder.forDouble.contramap(_.tss)
  given Decoder[TrainingStressScore] = Decoder.forDouble.map(TrainingStressScore.tss)
  given Encoder[IntensityFactor] = Encoder.forDouble.contramap(_.iff)
  given Decoder[IntensityFactor] = Decoder.forDouble.map(IntensityFactor.iff)
  given Encoder[SwimStroke] = Encoder.forString.contramap(_.typeName)
  given Decoder[SwimStroke] = Decoder.forString.emap(s =>
    SwimStroke.byTypeName(s).toRight(s"Invalid swim stroke: $s")
  )
  given Encoder[StrokesPerLap] = Encoder.forDouble.contramap(_.spl)
  given Decoder[StrokesPerLap] = Decoder.forDouble.map(StrokesPerLap.spl)
  given Encoder[Percent] = Encoder.forDouble.contramap(_.percent)
  given Decoder[Percent] = Decoder.forDouble.map(Percent.percent)
  given Encoder[ActivitySession] = deriveEncoder
  given Decoder[ActivitySession] = deriveDecoder
  given Encoder[DSession] = deriveEncoder
  given Decoder[DSession] = deriveDecoder

  given Encoder[LapTrigger] = Encoder.forString.contramap(_.typeName)
  given Decoder[LapTrigger] = Decoder.forString.emap(s =>
    LapTrigger.byTypeName(s).toRight(s"Invalid lap trigger: $s")
  )
  given Encoder[ActivityLap] = deriveEncoder
  given Decoder[ActivityLap] = deriveDecoder
  given Encoder[DLap] = deriveEncoder
  given Decoder[DLap] = deriveDecoder

  given Encoder[Grade] = Encoder.forDouble.contramap(_.percent)
  given Decoder[Grade] = Decoder.forDouble.map(Grade.percent)
  given Encoder[ActivitySessionData] = deriveEncoder
  given Decoder[ActivitySessionData] = deriveDecoder
  given Encoder[DSessionData] = deriveEncoder
  given Decoder[DSessionData] = deriveDecoder

  given Encoder[RActivityTag] = deriveEncoder
  given Decoder[RActivityTag] = deriveDecoder
  given Encoder[DActivityTag] = deriveEncoder
  given Decoder[DActivityTag] = deriveDecoder

  given Encoder[RActivityStrava] = deriveEncoder
  given Decoder[RActivityStrava] = deriveDecoder
  given Encoder[DActivityStrava] = deriveEncoder
  given Decoder[DActivityStrava] = deriveDecoder

  given Encoder[GeoPlace] = deriveEncoder
  given Decoder[GeoPlace] = deriveDecoder
  given Encoder[DGeoPlace] = deriveEncoder
  given Decoder[DGeoPlace] = deriveDecoder

  given Encoder[PositionName] = Encoder.forString.contramap(_.name)
  given Decoder[PositionName] = Decoder.forString.emap(PositionName.fromString)
  given Encoder[RActivityGeoPlace] = deriveEncoder
  given Decoder[RActivityGeoPlace] = deriveDecoder
  given Encoder[DActivityGeoPlace] = deriveEncoder
  given Decoder[DActivityGeoPlace] = deriveDecoder

  given Encoder[RStravaToken] = deriveEncoder
  given Decoder[RStravaToken] = deriveDecoder
  given Encoder[DStravaToken] = deriveEncoder
  given Decoder[DStravaToken] = deriveDecoder

  given Encoder[DumpFormat] = deriveEncoder
  given Decoder[DumpFormat] = deriveDecoder
