package fit4s.core

import java.time.{Duration as _, *}

import fit4s.core.Activity.{Record, Session, SessionRecords}
import fit4s.core.MessageReader as MR
import fit4s.core.data.*
import fit4s.profile.ActivityMsg
import fit4s.profile.FileType
import fit4s.profile.RecordMsg
import fit4s.profile.SessionMsg

/** Example for decoding an activity fit file.
  * https://developer.garmin.com/fit/file-types/activity/
  */
final case class Activity(
    timestamp: Instant,
    totalTime: Duration,
    sessions: Vector[Session],
    records: Vector[Record]
):
  lazy val sessionRecords: Vector[SessionRecords] =
    if sessions.sizeIs == 1 then Vector(SessionRecords(sessions.head, records))
    else splitRecords(sessions, records, Vector.empty)

  lazy val unrelatedRecords: Vector[Record] =
    if sessions.isEmpty then records
    else
      val span = sessions.map(_.timespan).reduce(_.union(_))
      records.filterNot(r => span.contains(r.timestamp))

  private def splitRecords(
      s: Vector[Session],
      rs: Vector[Record],
      result: Vector[SessionRecords]
  ): Vector[SessionRecords] =
    s match
      case a +: rest =>
        val (ars, remain) = rs.span(r => a.timespan.contains(r.timestamp))
        splitRecords(rest, remain, result.appended(SessionRecords(a, ars)))
      case _ => result

object Activity:
  given MR[Activity] = MR.forMsg(ActivityMsg) { m =>
    (MR.timestamp ::
      MR.field(m.totalTimerTime).as[Duration] ::
      MR.pure(Vector.empty) ::
      MR.pure(Vector.empty).tuple).as[Activity]
  }

  def from(fit: Fit): Either[String, Option[Activity]] =
    if fit.fileTypeIs(FileType.activity) then
      for
        am <- fit.getMessages(ActivityMsg).headOption.as[Activity]
        sessions <- fit.getMessages(SessionMsg).as[Session]
        records <- fit.getMessages(RecordMsg).as[Record]
      yield am.map(_.copy(sessions = sessions, records = records))
    else Right(None)

  final case class Session(
      timespan: Timespan,
      sport: String,
      avgSpeed: Option[Speed],
      avgHr: Option[HeartRate],
      totalDistance: Option[Distance],
      startPosition: Option[Position]
  )
  object Session:
    given MR[Session] =
      MR.forMsg(SessionMsg) { m =>
        val posRead = Position.reader(m.startPositionLat, m.startPositionLong)
        (MR.timespan ::
          MR.field(m.sport).asEnum.map(_.value) ::
          MR.field(m.enhancedAvgSpeed)
            .or(MR.field(m.avgSpeed))
            .as[Speed]
            .option ::
          MR.field(m.avgHeartRate).as[HeartRate].option ::
          MR.field(m.totalDistance).as[Distance].option ::
          posRead.option.tuple).as[Session]
      }

  final case class Record(
      timestamp: Instant,
      distance: Option[Distance],
      speed: Option[Speed],
      hr: Option[HeartRate],
      position: Option[Position],
      temperature: Option[Temperature]
  )

  object Record:
    given MR[Record] = MR.forMsg(RecordMsg) { m =>
      val posRead = Position.reader(m.positionLat, m.positionLong)
      (MR.timestamp :: MR.field(m.distance).as[Distance].option ::
        MR.field(m.enhancedSpeed)
          .or(MR.field(m.speed))
          .as[Speed]
          .option ::
        MR.field(m.heartRate).as[HeartRate].option ::
        posRead.option ::
        MR.field(m.temperature).as[Temperature].option.tuple).as[Record]
    }

  final case class SessionRecords(session: Session, records: Vector[Record])
