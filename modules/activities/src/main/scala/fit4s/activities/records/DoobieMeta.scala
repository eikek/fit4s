package fit4s.activities.records

import doobie.Meta
import fit4s.data._
import fit4s.profile.types.{LapTrigger, Sport, SubSport, SwimStroke}
import fs2.io.file.Path
import doobie.implicits.javatimedrivernative
import fit4s.activities.data._

import java.time.{Duration, Instant}

trait DoobieMeta {
  implicit val countryCodeMeta: Meta[CountryCode] =
    Meta[String].timap(CountryCode.apply)(_.cc)

  implicit val postCodeMeta: Meta[PostCode] =
    Meta[String].timap(PostCode.apply)(_.zip)

  implicit val activityGeoPlaceIdMeta: Meta[ActivityGeoPlaceId] =
    Meta[Long].timap(ActivityGeoPlaceId.apply)(_.id)

  implicit val geoPlaceIdMeta: Meta[GeoPlaceId] =
    Meta[Long].timap(GeoPlaceId.apply)(_.id)

  implicit val lapTriggerMeta: Meta[LapTrigger] =
    Meta[Long].timap(LapTrigger.unsafeByRawValue)(_.rawValue)

  implicit val swimStrokeMeta: Meta[SwimStroke] =
    Meta[Long].timap(SwimStroke.unsafeByRawValue)(_.rawValue)

  implicit val tssMeta: Meta[TrainingStressScore] =
    Meta[Double].timap(TrainingStressScore.tss)(_.tss)

  implicit val intensityFactorMeta: Meta[IntensityFactor] =
    Meta[Double].timap(IntensityFactor.iff)(_.iff)

  implicit val strokesPerLapMeta: Meta[StrokesPerLap] =
    Meta[Double].timap(StrokesPerLap.strokesPerLap)(_.spl)

  implicit val deviceProductMeta: Meta[DeviceProduct] =
    Meta[String].timap(DeviceProduct.unsafeFromString)(_.name)

  implicit val pathMeta: Meta[Path] =
    Meta[String].timap(Path.apply)(_.absolute.toString)

  implicit val fileIdMeta: Meta[FileId] =
    Meta[String].timap(FileId.unsafeFromString)(_.asString)

  implicit val sportMeta: Meta[Sport] =
    Meta[Long].timap(Sport.unsafeByRawValue)(_.rawValue)

  implicit val subSportMeta: Meta[SubSport] =
    Meta[Long].timap(SubSport.unsafeByRawValue)(_.rawValue)

  implicit val instantMeta: Meta[Instant] =
    javatimedrivernative.JavaTimeInstantMeta

  implicit val durationMeta: Meta[Duration] =
    Meta[Long].timap(Duration.ofMillis)(_.toMillis)

  implicit val distanceMeta: Meta[Distance] =
    Meta[Double].timap(Distance.meter)(_.meter)

  implicit val heartRateMeta: Meta[HeartRate] =
    Meta[Int].timap(HeartRate.bpm)(_.bpm)

  implicit val speedMeta: Meta[Speed] =
    Meta[Double].timap(Speed.meterPerSecond)(_.meterPerSecond)

  implicit val caloriesMeta: Meta[Calories] =
    Meta[Double].timap(Calories.kcal)(_.kcal)

  implicit val temperatureMeta: Meta[Temperature] =
    Meta[Double].timap(Temperature.celcius)(_.celcius)

  implicit val semicircleMeta: Meta[Semicircle] =
    Meta[Long].timap(Semicircle.semicircle)(_.semicircle)

  implicit val cadenceMeta: Meta[Cadence] =
    Meta[Int].timap(Cadence.rpm)(_.rpm)

  implicit val gradeMeta: Meta[Grade] =
    Meta[Double].timap(Grade.percent)(_.percent)

  implicit val powerMeta: Meta[Power] =
    Meta[Int].timap(Power.watts)(_.watts)

  implicit val tagNameMeta: Meta[TagName] =
    Meta[String].timap(TagName.unsafeFromString)(_.name)

  implicit val tagIdMeta: Meta[TagId] =
    Meta[Long].timap(TagId.apply)(_.id)

  implicit val locationIdMeta: Meta[LocationId] =
    Meta[Long].timap(LocationId.apply)(_.id)

  implicit val activityIdMeta: Meta[ActivityId] =
    Meta[Long].timap(ActivityId.apply)(_.id)

  implicit val activityTagIdMeta: Meta[ActivityTagId] =
    Meta[Long].timap(ActivityTagId.apply)(_.id)

  implicit val activityDataIdMeta: Meta[ActivitySessionDataId] =
    Meta[Long].timap(ActivitySessionDataId.apply)(_.id)

  implicit val activitySessionId: Meta[ActivitySessionId] =
    Meta[Long].timap(ActivitySessionId.apply)(_.id)
}

object DoobieMeta extends DoobieMeta
