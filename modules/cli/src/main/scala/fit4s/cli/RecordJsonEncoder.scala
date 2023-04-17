package fit4s.cli

import fit4s.activities.data._
import fit4s.activities.records._
import fit4s.cli.DataJsonEncoder._
import fit4s.geocode.{BoundingBox, NominatimOsmId, NominatimPlaceId}
import fit4s.profile.types.LapTrigger

import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import io.circe.{Encoder, Json, KeyEncoder}

object RecordJsonEncoder {

  def encodeList(r: ActivityListResult): Json =
    Json.obj(
      "activity" -> (r.activity -> r.location).asJson,
      "sessions" -> r.sessions.asJson,
      "tags" -> r.tags.asJson
    )

  def encodeDetail(r: ActivityDetailResult): Json =
    Json.obj(
      "activity" -> (r.activity -> r.location).asJson,
      "sessions" -> r.sessions.asJson,
      "tags" -> r.tags.asJson,
      "stravaId" -> r.stravaId.asJson,
      "laps" -> r.laps.asJson,
      "startPlace" -> r.startPlace.asJson,
      "endPlace" -> r.endPlace.asJson,
      "startEndDistance" -> r.startEndDistance.asJson
    )

  implicit val locationAndCountEncoder: Encoder[(RActivityLocation, Long)] =
    Encoder.instance { case (loc, count) =>
      Json.obj(
        "location" -> Json.obj(
          "id" -> loc.id.asJson,
          "path" -> loc.location.asJson
        ),
        "count" -> count.asJson
      )
    }

  implicit val geoPlaceEncoder: Encoder[RGeoPlace] =
    Encoder.instance { p =>
      Json.obj(
        "osmPlaceId" -> p.osmPlaceId.asJson,
        "osmId" -> p.osmId.asJson,
        "position" -> p.position.asJson,
        "road" -> p.road.asJson,
        "location" -> p.location.asJson,
        "country" -> p.country.asJson,
        "countryCode" -> p.countryCode.asJson,
        "postCode" -> p.postCode.asJson,
        "boundingBox" -> p.boundingBox.asJson
      )
    }

  implicit def lapEncoder: Encoder[RActivityLap] =
    Encoder.instance { l =>
      implicit val sport = l.sport
      Json.obj(
        "sport" -> l.sport.asJson,
        "subSport" -> l.subSport.asJson,
        "trigger" -> l.trigger.asJson,
        "startTime" -> l.startTime.asJson,
        "endTime" -> l.endTime.asJson,
        "movingTime" -> l.movingTime.asJson,
        "elapsedTime" -> l.elapsedTime.asJson,
        "distance" -> l.distance.asJson,
        "startPosition" -> l.startPosition.asJson,
        "endPosition" -> l.endPosition.asJson,
        "calories" -> l.calories.asJson,
        "totalAscend" -> l.totalAscend.asJson,
        "totalDescend" -> l.totalDescend.asJson,
        "minTemp" -> l.minTemp.asJson,
        "maxTemp" -> l.maxTemp.asJson,
        "avgTemp" -> l.avgTemp.asJson,
        "minHr" -> l.minHr.asJson,
        "maxHr" -> l.maxHr.asJson,
        "avgHr" -> l.avgHr.asJson,
        "maxSpeed" -> l.maxSpeed.asJson,
        "avgSpeed" -> l.avgSpeed.asJson,
        "maxPower" -> l.maxPower.asJson,
        "avgPower" -> l.avgPower.asJson,
        "normPower" -> l.normPower.asJson,
        "maxCadence" -> l.maxCadence.asJson,
        "avgCadence" -> l.avgCadence.asJson,
        "numPoolLengths" -> l.numPoolLength.asJson,
        "swimStroke" -> l.swimStroke.asJson,
        "avgStrokeDistance" -> l.avgStrokeDistance.asJson,
        "avgStrokeCount" -> l.strokeCount.asJson,
        "avgGrade" -> l.avgGrade.asJson
      )
    }

  implicit def sessionEncoder: Encoder[RActivitySession] =
    Encoder.instance { s =>
      implicit val sport = s.sport
      Json.obj(
        "sport" -> s.sport.asJson,
        "subSport" -> s.subSport.asJson,
        "startTime" -> s.startTime.asJson,
        "endTime" -> s.endTime.asJson,
        "movingTime" -> s.movingTime.asJson,
        "elapsedTime" -> s.elapsedTime.asJson,
        "distance" -> s.distance.asJson,
        "startPosition" -> s.startPosition.asJson,
        "calories" -> s.calories.asJson,
        "totalAscend" -> s.totalAscend.asJson,
        "totalDescend" -> s.totalDescend.asJson,
        "minTemp" -> s.minTemp.asJson,
        "maxTemp" -> s.maxTemp.asJson,
        "avgTemp" -> s.avgTemp.asJson,
        "minHr" -> s.minHr.asJson,
        "maxHr" -> s.maxHr.asJson,
        "avgHr" -> s.avgHr.asJson,
        "maxSpeed" -> s.maxSpeed.asJson,
        "avgSpeed" -> s.avgSpeed.asJson,
        "maxPower" -> s.maxPower.asJson,
        "avgPower" -> s.avgPower.asJson,
        "normPower" -> s.normPower.asJson,
        "maxCadence" -> s.maxCadence.asJson,
        "avgCadence" -> s.avgCadence.asJson,
        "numPoolLengths" -> s.numPoolLength.asJson,
        "if" -> s.iff.asJson,
        "swimStroke" -> s.swimStroke.asJson,
        "avgStrokeDistance" -> s.avgStrokeDistance.asJson,
        "avgStrokeCount" -> s.avgStrokeCount.asJson,
        "poolLength" -> s.poolLength.asJson,
        "avgGrade" -> s.avgGrade.asJson
      )
    }

  implicit def activityEncoder: Encoder[(RActivity, RActivityLocation)] =
    Encoder.instance { case (a, loc) =>
      Json.obj(
        "id" -> a.id.asJson,
        "location" -> loc.location.asJson,
        "file" -> (loc.location / a.path).asJson,
        "fileId" -> a.activityFileId.asJson,
        "fileIdString" -> a.activityFileId.asString.asJson,
        "created" -> a.created.asJson,
        "name" -> a.name.asJson,
        "timestamp" -> a.timestamp.asJson,
        "totalTime" -> a.totalTime.asJson,
        "notes" -> a.notes.asJson,
        "importedAt" -> a.importDate.asJson
      )
    }

  implicit def tagEncoder: Encoder[RTag] =
    Encoder.encodeString.contramap(_.name.name)

  implicit val triggerEncoder: Encoder[LapTrigger] =
    typedValueEncoder[LapTrigger]

  implicit val activitySessionIdEncoder: Encoder[ActivitySessionId] =
    Encoder.encodeLong.contramap(_.id)

  implicit val activitySessionIdKeyEncoder: KeyEncoder[ActivitySessionId] =
    KeyEncoder.instance(_.id.toString)

  implicit val nominatimPlaceIdEncoder: Encoder[NominatimPlaceId] =
    Encoder.encodeLong.contramap(_.id)

  implicit val nominatimOsmIdEncoder: Encoder[NominatimOsmId] =
    Encoder.encodeLong.contramap(_.id)

  implicit val countryCodeEncoder: Encoder[CountryCode] =
    Encoder.encodeString.contramap(_.cc)

  implicit val postCodeEncoder: Encoder[PostCode] =
    Encoder.encodeString.contramap(_.zip)

  implicit val boundingBoxEncoder: Encoder[BoundingBox] =
    deriveEncoder

  implicit val locationIdEncoder: Encoder[LocationId] =
    Encoder.encodeLong.contramap(_.id)
}
