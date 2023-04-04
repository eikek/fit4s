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
import fit4s.profile.types.DateTime

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
      fromRecords <- Sync[ConnectionIO].pure(FromRecords(result.recordsFor(session)))
      sessionId <- ActivitySessionRecord.insert(
        ActivitySessionRecord(
          id = ActivitySessionId(-1),
          activityId = activityId,
          sport = session.sport,
          subSport = session.subSport,
          startTime = session.startTime.asInstant,
          endTime = session.endTime.asInstant,
          movingTime = session.movingTime.getOrElse(fromRecords.duration),
          elapsedTime = session.elapsedTime.getOrElse(fromRecords.duration),
          distance = session.distance,
          startPosition = session.startPosition,
          calories = session.calories,
          totalAscend = session.totalAscend,
          totalDescend = session.totalDescend,
          minTemp = session.minTemp.orElse(fromRecords.minTemp),
          maxTemp = session.maxTemp.orElse(fromRecords.maxTemp),
          avgTemp = session.avgTemp.orElse(fromRecords.avgTemp),
          minHr = session.minHr.orElse(fromRecords.minHr),
          maxHr = session.maxHr.orElse(fromRecords.maxHr),
          avgHr = session.avgHr.orElse(fromRecords.avgHr),
          maxSpeed = Some(session.maxSpeed),
          avgSpeed = session.avgSpeed.orElse(fromRecords.maxSpeed),
          maxPower = session.maxPower.orElse(fromRecords.maxPower),
          avgPower = session.avgPower.orElse(fromRecords.avgPower)
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

  private case class FromRecords(
      minTime: DateTime = DateTime(0),
      maxTime: DateTime = DateTime(0),
      minHr: Option[HeartRate] = None,
      maxHr: Option[HeartRate] = None,
      avgHr: Option[HeartRate] = None,
      minTemp: Option[Temperature] = None,
      maxTemp: Option[Temperature] = None,
      avgTemp: Option[Temperature] = None,
      maxSpeed: Option[Speed] = None,
      avgSpeed: Option[Speed] = None,
      maxPower: Option[Power] = None,
      avgPower: Option[Power] = None
  ) {
    def duration: Duration = Duration.ofSeconds(maxTime.rawValue - minTime.rawValue)
  }
  private object FromRecords {
    def apply(rs: Vector[Record]): FromRecords = {
      val (hr, temp, pwr, spd, res) =
        rs.foldLeft(
          (
            Vector.empty[Int],
            Vector.empty[Double],
            Vector.empty[Int],
            Vector.empty[Double],
            FromRecords()
          )
        ) { case ((hr, temp, pwr, spd, res), r) =>
          (
            hr.appendedAll(r.heartRate.map(_.bpm)),
            temp.appendedAll(r.temperature.map(_.celcius)),
            pwr.appendedAll(r.power.map(_.watts)),
            spd.appendedAll(r.speed.map(_.meterPerSecond)),
            res.copy(
              minTime = Ordering[DateTime].min(res.minTime, r.timestamp),
              maxTime = Ordering[DateTime].max(res.maxTime, r.timestamp),
              minHr = selectMin(res.minHr, r.heartRate),
              maxHr = selectMax(res.maxHr, r.heartRate),
              minTemp = selectMin(res.minTemp, r.temperature),
              maxTemp = selectMax(res.maxTemp, r.temperature),
              maxSpeed = selectMax(res.maxSpeed, r.speed),
              maxPower = selectMax(res.maxPower, r.power)
            )
          )
        }

      val avgHr = if (hr.nonEmpty) Some(hr.sum / hr.size) else None
      val avgTemp = if (temp.nonEmpty) Some(temp.sum / temp.size) else None
      val avgPwr = if (pwr.nonEmpty) Some(pwr.sum / pwr.size) else None
      val avgSpd = if (spd.nonEmpty) Some(spd.sum / spd.size) else None

      res.copy(
        avgPower = avgPwr.map(Power.watts),
        avgHr = avgHr.map(HeartRate.bpm),
        avgTemp = avgTemp.map(Temperature.celcius),
        avgSpeed = avgSpd.map(Speed.meterPerSecond)
      )
    }

    private def selectMin[A](a: Option[A], b: Option[A])(implicit ordering: Ordering[A]) =
      (a, b) match {
        case (Some(x), Some(y)) => ordering.min(x, y).some
        case (x, None)          => x
        case (None, y)          => y
      }

    private def selectMax[A](a: Option[A], b: Option[A])(implicit ordering: Ordering[A]) =
      (a, b) match {
        case (Some(x), Some(y)) => ordering.max(x, y).some
        case (x, None)          => x
        case (None, y)          => y
      }
  }
}
