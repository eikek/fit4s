package fit4s.cli

import fit4s.activities.data.ActivityListResult
import fit4s.activities.records._
import io.circe.syntax._
import io.circe.{Encoder, Json}
import DataJsonEncoder._

object ActivityListResultJson {

  def encode(r: ActivityListResult): Json =
    Json.obj(
      "activity" -> (r.activity -> r.location).asJson,
      "sessions" -> r.sessions.asJson,
      "tags" -> r.tags.asJson
    )

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

}
