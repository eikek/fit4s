package fit4s

import fit4s.data.{Activity, ActivitySession, FileId}
import fit4s.profile.messages.{RecordMsg, SessionMsg}
import fit4s.profile.types.{DateTime, MesgNum}
import fit4s.util._

object ActivityReader {

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

  def read(fit: FitFile): Either[String, Result] = for {
    fileId <- fit.findFirstData(MesgNum.FileId).flatMap(FileId.from)
    am <- fit.findFirstData(MesgNum.Activity).flatMap(Activity.from)
    records <- fit.dataRecords
      .filter(_.isMessage(RecordMsg))
      .mapEither(data.Record.from)
    sessions <- fit.dataRecords
      .filter(_.isMessage(SessionMsg))
      .mapEither(ActivitySession.from)

    recs = records.groupBy { r =>
      sessions.find(_.containsTime(r.timestamp)).map(_.startTime)
    }
  } yield Result(fileId, am, sessions, recs)
}
