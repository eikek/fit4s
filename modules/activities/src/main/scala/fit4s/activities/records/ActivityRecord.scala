package fit4s.activities.records

import fit4s.activities.data.{ActivityId, LocationId}
import doobie.implicits._
import doobie._
import DoobieMeta._
import fit4s.data.FileId

import java.time.{Duration, Instant}

case class ActivityRecord(
    id: ActivityId,
    locationId: LocationId,
    path: String,
    activityFileId: FileId,
    name: String,
    timestamp: Instant,
    totalTime: Duration,
    notes: Option[String]
)

object ActivityRecord {
  private[activities] val table = fr"activity"
  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("location_id"),
      c("path"),
      c("file_id"),
      c("name"),
      c("timestamp"),
      c("total_time"),
      c("notes")
    )
  }
  private val columnsWithId =
    columnList(None).foldSmash1(Fragment.empty, sql",", Fragment.empty)

  def insert(r: ActivityRecord): ConnectionIO[ActivityId] =
    (sql"INSERT INTO $table (location_id, path, file_id, name, timestamp, total_time, notes) VALUES " ++
      sql"(${r.locationId}, ${r.path}, ${r.activityFileId}, ${r.name}, ${r.timestamp}, ${r.totalTime}, ${r.notes})").update
      .withUniqueGeneratedKeys[ActivityId]("id")

  def findById(id: ActivityId): ConnectionIO[Option[ActivityRecord]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[ActivityRecord]
      .option

  def findByFileId(fileId: FileId): ConnectionIO[Option[ActivityRecord]] =
    sql"SELECT $columnsWithId FROM $table WHERE file_id = $fileId"
      .query[ActivityRecord]
      .option
}
