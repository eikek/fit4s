package fit4s.activities.records

import java.time.Instant

import scala.collection.immutable.Seq

import fs2.Stream

import fit4s.activities.data.{
  ActivitySessionData,
  ActivitySessionDataId,
  ActivitySessionId
}
import fit4s.activities.records.DoobieImplicits.*
import fit4s.data.*

import doobie.*
import doobie.implicits.*

object RActivitySessionData:
  private[activities] val table = fr"activity_session_data"
  private[activities] def columnList(alias: Option[String]): List[Fragment] =
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))

    List(
      c("id"),
      c("activity_session_id"),
      c("timestamp"),
      c("position_lat"),
      c("position_long"),
      c("altitude"),
      c("heartrate"),
      c("cadence"),
      c("distance"),
      c("speed"),
      c("power"),
      c("grade"),
      c("temperature"),
      c("calories")
    )

  private val columnsNoId = columnList(None).tail.commas
  private val columnsWithId = columnList(None).commas

  def insert(r: ActivitySessionData): ConnectionIO[ActivitySessionDataId] =
    (sql"INSERT INTO $table ($columnsNoId) VALUES (" ++
      sql"${r.activitySessionId}, ${r.timestamp}, ${r.position.map(_.latitude)}, " ++
      sql"${r.position.map(_.longitude)}, ${r.altitude}, ${r.heartRate}, " ++
      sql"${r.cadence}, ${r.distance}, ${r.speed}, " ++
      sql"${r.power}, ${r.grade}, ${r.temperature}, ${r.calories}" ++
      sql")").update
      .withUniqueGeneratedKeys[ActivitySessionDataId]("id")

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[ActivitySessionData]): ConnectionIO[Int] =
      Update[ActivitySessionData](sql).updateMany(tags)

  def findForSession(id: ActivitySessionId): ConnectionIO[List[ActivitySessionData]] =
    sql"""SELECT $columnsWithId
          FROM $table
          WHERE activity_session_id = $id
          ORDER BY timestamp"""
      .query[ActivitySessionData]
      .to[List]

  def countAll =
    sql"SELECT count(*) FROM $table".query[Long].unique

  def streamAll: Stream[ConnectionIO, ActivitySessionData] =
    sql"SELECT $columnsWithId FROM $table"
      .query[ActivitySessionData]
      .streamWithChunkSize(100)
