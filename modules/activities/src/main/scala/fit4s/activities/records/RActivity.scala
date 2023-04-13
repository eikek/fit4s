package fit4s.activities.records

import fit4s.activities.data.{ActivityId, LocationId}
import doobie.implicits._
import doobie._
import DoobieImplicits._
import cats.data.NonEmptyList
import cats.effect.kernel.Clock
import fit4s.activities.impl.ActivityName
import fit4s.data.{DeviceProduct, FileId}

import java.time.{Duration, Instant, ZoneId}

case class RActivity(
    id: ActivityId,
    locationId: LocationId,
    path: String,
    activityFileId: FileId,
    device: DeviceProduct,
    serialNumber: Option[Long],
    created: Option[Instant],
    name: String,
    timestamp: Instant,
    totalTime: Duration,
    notes: Option[String],
    importDate: Instant
)

object RActivity {
  def apply(
      id: ActivityId,
      locationId: LocationId,
      path: String,
      activityFileId: FileId,
      name: String,
      timestamp: Instant,
      totalTime: Duration,
      notes: Option[String],
      importDate: Instant
  ): RActivity = RActivity(
    id,
    locationId,
    path,
    activityFileId,
    activityFileId.product,
    activityFileId.serialNumber,
    activityFileId.createdAt.map(_.asInstant),
    name,
    timestamp,
    totalTime,
    notes,
    importDate
  )

  private[activities] val table = fr"activity"
  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("location_id"),
      c("path"),
      c("file_id"),
      c("device"),
      c("serial_number"),
      c("created_at"),
      c("name"),
      c("timestamp"),
      c("total_time"),
      c("notes"),
      c("import_time")
    )
  }
  private val columnsWithId = columnList(None).commas
  private val columnsNoId = columnList(None).tail.commas

  def insert(r: RActivity): ConnectionIO[ActivityId] =
    (sql"INSERT INTO $table ($columnsNoId) VALUES " ++
      sql"(${r.locationId}, ${r.path}, ${r.activityFileId}, ${r.device}, " ++
      sql"${r.serialNumber}, ${r.created}, ${r.name}, ${r.timestamp}, ${r.totalTime}, " ++
      sql"${r.notes}, ${r.importDate})").update
      .withUniqueGeneratedKeys[ActivityId]("id")

  def findById(id: ActivityId): ConnectionIO[Option[RActivity]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[RActivity]
      .option

  def findByFileId(fileId: FileId): ConnectionIO[Option[RActivity]] =
    sql"SELECT $columnsWithId FROM $table WHERE file_id = $fileId"
      .query[RActivity]
      .option

  def latestImport: ConnectionIO[Option[Instant]] =
    sql"SELECT max(import_time) from $table".query[Instant].option

  def updateName(id: ActivityId, name: String): ConnectionIO[Int] =
    sql"UPDATE $table SET name = $name WHERE id = $id".update.run

  def setGeneratedName(id: ActivityId, zoneId: ZoneId): ConnectionIO[Int] =
    for {
      sports <- RActivitySession.sportsByActivity(id)
      start <- RActivitySession.lowestStartTime(id)
      now <- Clock[ConnectionIO].realTimeInstant
      name = ActivityName.generate(start.getOrElse(now), sports, zoneId)
      n <- updateName(id, name)
    } yield n

  def updateNotes(id: ActivityId, desc: Option[String]): ConnectionIO[Int] =
    sql"UPDATE $table SET notes = $desc WHERE id = $id".update.run

  def delete(ids: NonEmptyList[ActivityId]): ConnectionIO[Int] = {
    val in = ids.toList.map(id => sql"$id").commas
    sql"DELETE FROM $table WHERE id in ($in)".update.run
  }
}
