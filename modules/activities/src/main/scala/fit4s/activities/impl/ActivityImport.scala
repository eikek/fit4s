package fit4s.activities.impl

import cats.data.{NonEmptyList, OptionT}
import cats.effect.kernel.Sync
import cats.syntax.all._
import doobie._
import fit4s.ActivityReader
import fit4s.activities.ImportResult
import fit4s.activities.data._
import fit4s.activities.records._
import fit4s.data._

import java.time.{Duration, ZoneId}

object ActivityImport {
  def addActivity(
      tags: Set[TagId],
      locationId: LocationId,
      path: String,
      notes: Option[String],
      zone: ZoneId
  )(
      result: ActivityReader.Result
  ): ConnectionIO[ImportResult[ActivityId]] = {
    val sports = result.sessions.map(_.sport).toSet
    val name = ActivityName.generate(result.activity.timestamp, sports, zone)

    for {
      actId <- add(locationId, path, name, notes)(result)
      _ <- NonEmptyList.fromList(tags.toList) match {
        case Some(nel) =>
          actId match {
            case ImportResult.Success(id) => ActivityTagRecord.insert(id, nel)
            case _                        => Sync[ConnectionIO].pure(0)
          }
        case None => Sync[ConnectionIO].pure(0)
      }
    } yield actId
  }

  def add(locationId: LocationId, path: String, name: String, notes: Option[String])(
      result: ActivityReader.Result
  ): ConnectionIO[ImportResult[ActivityId]] = {
    val insert: ConnectionIO[ImportResult[ActivityId]] =
      for {
        actId <- ActivityRecord.insert(
          ActivityRecord(
            ActivityId(-1),
            locationId,
            path,
            result.fileId,
            name,
            result.activity.timestamp.asInstant,
            result.activity.totalTime,
            notes
          )
        )

        _ <- result.sessions.traverse_(addSession(actId, result))
      } yield ImportResult.success(actId)

    OptionT(ActivityRecord.findByFileId(result.fileId))
      .map(_ => ImportResult.duplicate(result.fileId, path))
      .getOrElseF(insert)
  }

  private def addSession(activityId: ActivityId, result: ActivityReader.Result)(
      session: ActivitySession
  ): ConnectionIO[(ActivitySessionId, Vector[ActivitySessionDataId])] =
    for {
      sessionId <- ActivitySessionRecord.insert(
        ActivitySessionRecord(
          id = ActivitySessionId(-1),
          activityId = activityId,
          sport = session.sport,
          subSport = session.subSport,
          startTime = session.startTime.asInstant,
          endTime = session.endTime.asInstant,
          movingTime = session.movingTime.getOrElse(Duration.ZERO),
          elapsedTime = session.elapsedTime.getOrElse(Duration.ZERO),
          distance = session.distance,
          startPosition = session.startPosition,
          calories = session.calories,
          totalAscend = session.totalAscend,
          totalDescend = session.totalDescend,
          minTemp = session.minTemp,
          maxTemp = session.maxTemp,
          avgTemp = session.avgTemp,
          minHr = session.minHr,
          maxHr = session.maxHr,
          avgHr = session.avgHr,
          maxSpeed = Some(session.maxSpeed),
          avgSpeed = session.avgSpeed,
          maxPower = session.maxPower,
          avgPower = session.avgPower
        )
      )
      rIds <- result.recordsFor(session).traverse { r =>
        ActivitySessionDataRecord.insert(
          ActivitySessionDataRecord(
            id = ActivitySessionDataId(-1),
            activitySessionId = sessionId,
            timestamp = r.timestamp.asInstant,
            position = r.position,
            altitude = r.altitude,
            heartRate = r.heartRate,
            cadence = r.cadence,
            distance = r.distance,
            speed = r.speed,
            power = r.power,
            grade = r.grade,
            temperature = r.temperature,
            calories = r.calories
          )
        )
      }
    } yield (sessionId, rIds)
}
