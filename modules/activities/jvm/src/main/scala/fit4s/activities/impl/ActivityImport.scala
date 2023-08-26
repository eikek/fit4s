package fit4s.activities.impl

import java.time.{Duration, Instant, ZoneId}

import cats.data.{NonEmptyList, OptionT}
import cats.effect.Sync
import cats.syntax.all._

import fit4s.ActivityReader
import fit4s.activities.ImportResult
import fit4s.activities.data.Activity
import fit4s.activities.data._
import fit4s.activities.records._
import fit4s.data._

import doobie._

object ActivityImport {
  def addActivity(
      tags: Set[TagId],
      locationId: LocationId,
      path: String,
      notes: Option[String],
      zone: ZoneId,
      now: Instant
  )(
      result: ActivityReader.Result
  ): ConnectionIO[ImportResult[ActivityId]] = {
    val sports = result.sessions.map(_.sport).toSet
    val startTime =
      result.sessions.headOption.map(_.startTime).getOrElse(result.activity.timestamp)
    val name = ActivityName.generate(startTime, sports, zone)

    for {
      actId <- add(locationId, path, name, notes, now)(result)
      _ <- NonEmptyList.fromList(tags.toList) match {
        case Some(nel) =>
          actId match {
            case ImportResult.Success(id) => RActivityTag.insert1(id, nel)
            case _                        => Sync[ConnectionIO].pure(0)
          }
        case None => Sync[ConnectionIO].pure(0)
      }
    } yield actId
  }

  def add(
      locationId: LocationId,
      path: String,
      name: String,
      notes: Option[String],
      now: Instant
  )(
      result: ActivityReader.Result
  ): ConnectionIO[ImportResult[ActivityId]] = {
    val insert: ConnectionIO[ImportResult[ActivityId]] =
      for {
        actId <- RActivity.insert(
          Activity(
            ActivityId(-1),
            locationId,
            path,
            result.fileId,
            name,
            result.activity.timestamp.asInstant,
            result.activity.totalTime,
            notes,
            now
          )
        )

        _ <- result.sessions.traverse_(addSession(actId, result))
      } yield ImportResult.success(actId)

    OptionT(RActivity.findByFileId(result.fileId))
      .map(r => ImportResult.duplicate(r.id, result.fileId, path))
      .getOrElseF(insert)
  }

  private def addSession(activityId: ActivityId, result: ActivityReader.Result)(
      session: FitActivitySession
  ): ConnectionIO[
    (ActivitySessionId, Vector[ActivityLapId], Vector[ActivitySessionDataId])
  ] =
    for {
      sessionId <- RActivitySession.insert(
        ActivitySession(
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
          avgPower = session.avgPower,
          normPower = session.normPower,
          maxCadence = session.maxCadence,
          avgCadence = session.avgCadence,
          tss = session.trainingStressScore,
          numPoolLength = session.numPoolLength,
          iff = session.intensityFactor,
          swimStroke = session.swimStroke,
          avgStrokeDistance = session.avgStrokeDistance,
          avgStrokeCount = session.avgStrokeCount,
          poolLength = session.poolLength,
          avgGrade = session.avgGrade
        )
      )
      lapIds <- result.lapsFor(session).traverse { l =>
        RActivityLap.insert(
          ActivityLap(
            id = ActivityLapId(-1),
            activitySessionId = sessionId,
            sport = l.sport,
            subSport = l.subSport,
            trigger = l.trigger,
            startTime = l.startTime.asInstant,
            endTime = l.endTime.asInstant,
            startPosition = l.startPosition,
            endPosition = l.endPosition,
            movingTime = l.movingTime,
            elapsedTime = l.elapsedTime,
            calories = l.calories,
            distance = l.distance,
            minTemp = l.minTemp,
            maxTemp = l.maxTemp,
            avgTemp = l.avgTemp,
            maxSpeed = l.maxSpeed,
            avgSpeed = l.avgSpeed,
            minHr = l.minHr,
            maxHr = l.maxHr,
            avgHr = l.avgHr,
            maxPower = l.maxPower,
            avgPower = l.avgPower,
            normPower = l.normPower,
            maxCadence = l.maxCadence,
            avgCadence = l.avgCadence,
            totalAscend = l.totalAscend,
            totalDescend = l.totalDescend,
            numPoolLength = l.numPoolLength,
            swimStroke = l.swimStroke,
            avgStrokeDistance = l.avgStrokeDistance,
            strokeCount = l.strokeCount,
            avgGrade = l.avgGrade
          )
        )
      }
      rIds <- result.recordsFor(session).traverse { r =>
        RActivitySessionData.insert(
          ActivitySessionData(
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
    } yield (sessionId, lapIds, rIds)
}
