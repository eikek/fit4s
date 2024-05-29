package fit4s.activities.records

import java.time.{Instant, ZoneId}

import scala.collection.immutable.Seq

import cats.data.NonEmptyList
import cats.effect.kernel.Clock
import fs2.Stream

import fit4s.activities.StravaExternalId
import fit4s.activities.data.{Activity, ActivityId, LocationId}
import fit4s.activities.impl.ActivityName
import fit4s.activities.records.DoobieImplicits.*
import fit4s.data.FileId

import doobie.*
import doobie.implicits.*

object RActivity:
  private[activities] val table = fr"activity"
  private[activities] def columnList(alias: Option[String]): List[Fragment] =
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
  private val columnsWithId = columnList(None).commas
  private val columnsNoId = columnList(None).tail.commas

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[Activity]): ConnectionIO[Int] =
      Update[Activity](sql).updateMany(tags)

  def insert(r: Activity): ConnectionIO[ActivityId] =
    (sql"INSERT INTO $table ($columnsNoId) VALUES " ++
      sql"(${r.locationId}, ${r.path}, ${r.activityFileId}, ${r.device}, " ++
      sql"${r.serialNumber}, ${r.created}, ${r.name}, ${r.timestamp}, ${r.totalTime}, " ++
      sql"${r.notes}, ${r.importDate})").update
      .withUniqueGeneratedKeys[ActivityId]("id")

  def findById(id: ActivityId): ConnectionIO[Option[Activity]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[Activity]
      .option

  def findByFileId(fileId: FileId): ConnectionIO[Option[Activity]] =
    sql"SELECT $columnsWithId FROM $table WHERE file_id = $fileId"
      .query[Activity]
      .option

  def findByStravaExternalId(id: StravaExternalId): ConnectionIO[Option[Activity]] =
    sql"""SELECT $columnsWithId
           FROM $table a
           WHERE a.file_id = ${id.fileId} OR a.id = ${id.activityId}
           LIMIT 1"""
      .query[Activity]
      .option

  def streamAll: Stream[ConnectionIO, Activity] =
    sql"""SELECT $columnsWithId FROM $table""".query[Activity].streamWithChunkSize(100)

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

  def delete(ids: NonEmptyList[ActivityId]): ConnectionIO[Int] =
    val in = ids.toList.map(id => sql"$id").commas
    sql"DELETE FROM $table WHERE id in ($in)".update.run
