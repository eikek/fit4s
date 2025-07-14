package fit4s.activities.records

import java.time.{Duration, Instant}

import fs2.io.file.Path

import fit4s.activities.data.{Activity as FActivity, *}
import fit4s.data.*
import fit4s.geocode.data.*
import fit4s.profile.types.*
import fit4s.strava.data.*

import doobie.*
import doobie.syntax.all.*

trait DoobieMeta:
  given Get[ActivityLapId] = Get[Long].map(ActivityLapId.apply)
  given Put[ActivityLapId] = Put[Long].contramap(_.id)

  given Get[Percent] = Get[Double].map(Percent.percent)
  given Put[Percent] = Put[Double].contramap(_.percent)

  given Get[StravaActivityId] = Get[Long].map(StravaActivityId.apply)
  given Put[StravaActivityId] = Put[Long].contramap(_.id)

  given Get[StravaScope] = Get[String].map(StravaScope.apply)
  given Put[StravaScope] = Put[String].contramap(_.asString)

  given Get[ActivityStravaId] = Get[Long].map(ActivityStravaId.apply)
  given Put[ActivityStravaId] = Put[Long].contramap(_.id)

  given Get[StravaAccessToken] = Get[String].map(StravaAccessToken.apply)
  given Put[StravaAccessToken] = Put[String].contramap(_.token)

  given Get[StravaRefreshToken] = Get[String].map(StravaRefreshToken.apply)
  given Put[StravaRefreshToken] = Put[String].contramap(_.token)

  given Get[StravaTokenId] = Get[Long].map(StravaTokenId.apply)
  given Put[StravaTokenId] = Put[Long].contramap(_.id)

  given Get[PositionName] = Get[String].map(PositionName.unsafeFromString)
  given Put[PositionName] = Put[String].contramap(_.name)

  given Get[CountryCode] = Get[String].map(CountryCode.apply)
  given Put[CountryCode] = Put[String].contramap(_.code)

  given Get[PostCode] = Get[String].map(PostCode.apply)
  given Put[PostCode] = Put[String].contramap(_.zip)

  given Get[ActivityGeoPlaceId] = Get[Long].map(ActivityGeoPlaceId.apply)
  given Put[ActivityGeoPlaceId] = Put[Long].contramap(_.id)

  given Get[GeoPlaceId] = Get[Long].map(GeoPlaceId.apply)
  given Put[GeoPlaceId] = Put[Long].contramap(_.id)

  given Get[NominatimPlaceId] = Get[Long].map(NominatimPlaceId.apply)
  given Put[NominatimPlaceId] = Put[Long].contramap(_.id)

  given Get[NominatimOsmId] = Get[Long].map(NominatimOsmId.apply)
  given Put[NominatimOsmId] = Put[Long].contramap(_.id)

  given Get[LapTrigger] = Get[Long].map(LapTrigger.unsafeByRawValue)
  given Put[LapTrigger] = Put[Long].contramap(_.rawValue)

  given Get[SwimStroke] = Get[Long].map(SwimStroke.unsafeByRawValue)
  given Put[SwimStroke] = Put[Long].contramap(_.rawValue)

  given Get[TrainingStressScore] = Get[Double].map(TrainingStressScore.tss)
  given Put[TrainingStressScore] = Put[Double].contramap(_.tss)

  given Get[IntensityFactor] = Get[Double].map(IntensityFactor.iff)
  given Put[IntensityFactor] = Put[Double].contramap(_.iff)

  given Get[StrokesPerLap] = Get[Double].map(StrokesPerLap.spl)
  given Put[StrokesPerLap] = Put[Double].contramap(_.spl)

  given Get[DeviceProduct] = Get[String].map(DeviceProduct.unsafeFromString)
  given Put[DeviceProduct] = Put[String].contramap(_.name)

  given Get[DeviceInfo] = Get[String].map(DeviceInfo.fromString)
  given Put[DeviceInfo] = Put[String].contramap(_.name)

  given Get[Path] = Get[String].map(Path.apply)
  given Put[Path] = Put[String].contramap(_.absolute.toString)

  given Get[FileId] = Get[String].map(FileId.unsafeFromString)
  given Put[FileId] = Put[String].contramap(_.asString)

  given Get[Sport] = Get[Long].map(Sport.unsafeByRawValue)
  given Put[Sport] = Put[Long].contramap(_.rawValue)

  given Get[SubSport] = Get[Long].map(SubSport.unsafeByRawValue)
  given Put[SubSport] = Put[Long].contramap(_.rawValue)

  given Get[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta.get
  given Put[Instant] = doobie.implicits.legacy.instant.JavaTimeInstantMeta.put

  given Get[Duration] = Get[Long].map(Duration.ofMillis)
  given Put[Duration] = Put[Long].contramap(_.toMillis)

  given Get[Distance] = Get[Double].map(Distance.meter)
  given Put[Distance] = Put[Double].contramap(_.meter)

  given Get[HeartRate] = Get[Int].map(HeartRate.bpm)
  given Put[HeartRate] = Put[Int].contramap(_.bpm)

  given Get[Speed] = Get[Double].map(Speed.meterPerSecond)
  given Put[Speed] = Put[Double].contramap(_.meterPerSecond)

  given Get[Calories] = Get[Double].map(Calories.kcal)
  given Put[Calories] = Put[Double].contramap(_.kcal)

  given Get[Temperature] = Get[Double].map(Temperature.celcius)
  given Put[Temperature] = Put[Double].contramap(_.celcius)

  given Get[Semicircle] = Get[Long].map(Semicircle.semicircle)
  given Put[Semicircle] = Put[Long].contramap(_.semicircle)

  given Get[Cadence] = Get[Int].map(Cadence.rpm)
  given Put[Cadence] = Put[Int].contramap(_.rpm)

  given Get[Grade] = Get[Double].map(Grade.percent)
  given Put[Grade] = Put[Double].contramap(_.percent)

  given Get[Power] = Get[Int].map(Power.watts)
  given Put[Power] = Put[Int].contramap(_.watts)

  given Get[TagName] = Get[String].map(TagName.unsafeFromString)
  given Put[TagName] = Put[String].contramap(_.name)

  given Get[TagId] = Get[Long].map(TagId.apply)
  given Put[TagId] = Put[Long].contramap(_.id)

  given Read[Tag] = Read.derived
  given Write[Tag] = Write.derived

  given Get[LocationId] = Get[Long].map(LocationId.apply)
  given Write[LocationId] = Write[Long].contramap(_.id)

  given Get[ActivityId] = Get[Long].map(ActivityId.apply)
  given Write[ActivityId] = Write[Long].contramap(_.id)

  given Get[ActivityTagId] = Get[Long].map(ActivityTagId.apply)
  given Write[ActivityTagId] = Write[Long].contramap(_.id)

  given Get[ActivitySessionDataId] = Get[Long].map(ActivitySessionDataId.apply)
  given Write[ActivitySessionDataId] = Write[Long].contramap(_.id)

  given Get[ActivitySessionId] = Get[Long].map(ActivitySessionId.apply)
  given Write[ActivitySessionId] = Write[Long].contramap(_.id)

  given Read[Position] = Read.derived
  given Write[Position] = Write.derived

  given Read[BoundingBox] = Read.derived
  given Write[BoundingBox] = Write.derived

  given Read[GeoPlace] = Read.derived
  given Write[GeoPlace] = Write.derived

  given Read[ActivitySessionData] = Read.derived
  given Write[ActivitySessionData] = Write.derived

  given Read[ActivitySession] = Read.derived
  given Write[ActivitySession] = Write.derived

  given Read[Location] = Read.derived
  given Write[Location] = Write.derived

  given Read[ActivityLap] = Read.derived
  given Write[ActivityLap] = Write.derived

  given Read[FActivity] = Read.derived
  given Write[FActivity] = Write.derived

  given Read[ActivitySessionSummary] = Read.derived
  given Write[ActivitySessionSummary] = Write.derived

  given Read[ActivityRereadData] = Read.derived

object DoobieMeta extends DoobieMeta
