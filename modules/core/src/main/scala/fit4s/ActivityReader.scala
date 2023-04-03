package fit4s

import fit4s.ActivityReader.Failure.GeneralError
import fit4s.data.{Activity, ActivitySession, FileId}
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
}
