package fit4s

import fit4s.ActivityReader.Failure.GeneralError
import fit4s.data._
import fit4s.profile.messages.{RecordMsg, SessionMsg}
import fit4s.profile.types.{DateTime, LocalDateTime, MesgNum}
import fit4s.util._

import java.time.{Duration, ZoneId}

object ActivityReader {
  sealed trait Failure extends Product
  object Failure {
    final case class NoFileIdFound(message: String) extends Failure
    final case class NoActivityFound(id: FileId, message: String) extends Failure
    final case class GeneralError(message: String) extends Failure
  }

  final case class Result(
      fileId: FileId,
      activity: Activity,
      sessions: Vector[ActivitySession],
      records: Map[Option[DateTime], Vector[data.Record]]
  ) {
    def unrelatedRecords = records.getOrElse(None, Vector.empty)
    def recordsFor(session: ActivitySession) =
      records.getOrElse(Some(session.startTime), Vector.empty)
  }

  def read(fit: FitFile, zoneId: ZoneId): Either[Failure, Result] = for {
    fileId <- fit.findFileId.left.map(Failure.NoFileIdFound)
    am <- fit
      .findFirstData(MesgNum.Activity)
      .flatMap(Activity.from)
      .left
      .map(Failure.NoActivityFound(fileId, _))
    records <- fit.dataRecords
      .filter(_.isMessage(RecordMsg))
      .mapEither(data.Record.from)
      .left
      .map(GeneralError)
    sessions <- fit.dataRecords
      .filter(_.isMessage(SessionMsg))
      .mapEither(ActivitySession.from)
      .left
      .map(GeneralError)

    recs = records.groupBy { r =>
      sessions.find(_.containsTime(r.timestamp)).map(_.startTime)
    }
  } yield tryFixTimestamps(Result(fileId, am, sessions, recs), zoneId)

  /** Sometimes values in the session message are missing. They can be filled by computing
    * them from all corresponding records.
    */
  def fixMissingValues(result: Result): Result =
    result.copy(sessions = result.sessions.map { s =>
      val fromRecords = FromRecords(result.recordsFor(s))
      s.copy(
        movingTime = s.movingTime.orElse(Some(fromRecords.duration)),
        elapsedTime = s.elapsedTime.orElse(Some(fromRecords.duration)),
        minTemp = s.minTemp.orElse(fromRecords.minTemp),
        maxTemp = s.maxTemp.orElse(fromRecords.maxTemp),
        avgTemp = s.avgTemp.orElse(fromRecords.avgTemp),
        minHr = s.minHr.orElse(fromRecords.minHr),
        maxHr = s.maxHr.orElse(fromRecords.maxHr),
        avgHr = s.avgHr.orElse(fromRecords.avgHr),
        avgSpeed = s.avgSpeed.orElse(fromRecords.maxSpeed),
        maxPower = s.maxPower.orElse(fromRecords.maxPower),
        avgPower = s.avgPower.orElse(fromRecords.avgPower)
      )
    })

  private[fit4s] def tryFixTimestamps(result: Result, zoneId: ZoneId): Result =
    (result.activity.timestamp.isTooLow, result.activity.localTimestamp) match {
      case (_, None)                              => result
      case (false, _)                             => result
      case (_, Some(localTs)) if localTs.isTooLow => result
      case (true, Some(localTs)) =>
        val wrongSecs = result.activity.timestamp.rawValue
        val zoned = localTs.asLocalDateTime.atZone(zoneId)
        val correctSecs = Duration.between(DateTime.offset, zoned).toSeconds
        val diff = correctSecs - wrongSecs
        Result(
          fileId = result.fileId.copy(createdAt = addOpt(result.fileId.createdAt, diff)),
          activity =
            result.activity.copy(timestamp = add(result.activity.timestamp, diff)),
          sessions = result.sessions.map(s =>
            s.copy(startTime = add(s.startTime, diff), endTime = add(s.endTime, diff))
          ),
          records = result.records.map { case (optDt, records) =>
            addOpt(optDt, diff) -> records.map(r =>
              r.copy(timestamp = add(r.timestamp, diff))
            )
          }
        )
    }

  private def addOpt(dt: Option[DateTime], secs: Long): Option[DateTime] =
    dt.map(add(_, secs))

  private def add(dt: DateTime, secs: Long): DateTime =
    if (dt.isTooLow) DateTime(dt.rawValue + secs) else dt

  implicit private class DateTimeOps(dt: DateTime) {
    def isTooLow = dt.rawValue < DateTime.minTimeForOffset
  }

  implicit private class LocalDateTimeOps(dt: LocalDateTime) {
    def isTooLow = dt.rawValue < LocalDateTime.minTimeForOffset
  }

  private case class FromRecords(
      minTime: DateTime = DateTime(Int.MaxValue),
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
        case (Some(x), Some(y)) => Some(ordering.min(x, y))
        case (x, None)          => x
        case (None, y)          => y
      }

    private def selectMax[A](a: Option[A], b: Option[A])(implicit ordering: Ordering[A]) =
      (a, b) match {
        case (Some(x), Some(y)) => Some(ordering.max(x, y))
        case (x, None)          => x
        case (None, y)          => y
      }
  }
}
