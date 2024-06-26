package fit4s.activities.records

import java.time.{Duration, Instant}

import fs2.io.file.Path

import fit4s.activities.data.*
import fit4s.data.*
import fit4s.geocode.data.*
import fit4s.profile.types.*
import fit4s.strava.data.*

import doobie.Meta

trait DoobieMeta:
  implicit val activityLapIdMeta: Meta[ActivityLapId] =
    Meta[Long].timap(ActivityLapId.apply)(_.id)

  implicit val percentMeta: Meta[Percent] =
    Meta[Double].timap(Percent.percent)(_.percent)

  implicit val stravaIdMeta: Meta[StravaActivityId] =
    Meta[Long].timap(StravaActivityId.apply)(_.id)

  implicit val stravaScopeMeta: Meta[StravaScope] =
    Meta[String].timap(StravaScope.apply)(_.asString)

  implicit val activityStravaIdMeta: Meta[ActivityStravaId] =
    Meta[Long].timap(ActivityStravaId.apply)(_.id)

  implicit val stravaAccessTokenMeta: Meta[StravaAccessToken] =
    Meta[String].timap(StravaAccessToken.apply)(_.token)

  implicit val stravaRefreshTokenMeta: Meta[StravaRefreshToken] =
    Meta[String].timap(StravaRefreshToken.apply)(_.token)

  implicit val stravaTokenIdMeta: Meta[StravaTokenId] =
    Meta[Long].timap(StravaTokenId.apply)(_.id)

  implicit val positionNameMeta: Meta[PositionName] =
    Meta[String].timap(PositionName.unsafeFromString)(_.name)

  implicit val countryCodeMeta: Meta[CountryCode] =
    Meta[String].timap(CountryCode.apply)(a => a.code)

  implicit val postCodeMeta: Meta[PostCode] =
    Meta[String].timap(PostCode.apply)(_.zip)

  implicit val activityGeoPlaceIdMeta: Meta[ActivityGeoPlaceId] =
    Meta[Long].timap(ActivityGeoPlaceId.apply)(_.id)

  implicit val geoPlaceIdMeta: Meta[GeoPlaceId] =
    Meta[Long].timap(GeoPlaceId.apply)(_.id)

  implicit val osmPlaceIdMeta: Meta[NominatimPlaceId] =
    Meta[Long].timap(NominatimPlaceId.apply)(_.id)

  implicit val osmIdMeta: Meta[NominatimOsmId] =
    Meta[Long].timap(NominatimOsmId.apply)(_.id)

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

  implicit val deviceInfoMeta: Meta[DeviceInfo] =
    Meta[String].timap(DeviceInfo.fromString)(_.name)

  implicit val pathMeta: Meta[Path] =
    Meta[String].timap(Path.apply)(_.absolute.toString)

  implicit val fileIdMeta: Meta[FileId] =
    Meta[String].timap(FileId.unsafeFromString)(_.asString)

  implicit val sportMeta: Meta[Sport] =
    Meta[Long].timap(Sport.unsafeByRawValue)(_.rawValue)

  implicit val subSportMeta: Meta[SubSport] =
    Meta[Long].timap(SubSport.unsafeByRawValue)(_.rawValue)

  implicit val instantMeta: Meta[Instant] =
    doobie.implicits.legacy.instant.JavaTimeInstantMeta

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

object DoobieMeta extends DoobieMeta
