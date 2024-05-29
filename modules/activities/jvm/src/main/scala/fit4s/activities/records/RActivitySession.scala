package fit4s.activities.records

import java.time.Instant

import scala.collection.immutable.Seq

import cats.syntax.all.*
import fs2.Stream

import fit4s.activities.data.ActivitySession
import fit4s.activities.data.{ActivityId, ActivitySessionId}
import fit4s.activities.records.DoobieImplicits.*
import fit4s.data.Position
import fit4s.profile.types.*

import doobie.*
import doobie.implicits.*

object RActivitySession:
  private[activities] val table = fr"activity_session"
  private val columns = columnList(None).tail.commas
  private val columnsWithId = columnList(None).commas

  private[activities] def columnList(alias: Option[String]): List[Fragment] =
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("activity_id"),
      c("sport"),
      c("sub_sport"),
      c("start_time"),
      c("end_time"),
      c("moving_time"),
      c("elapsed_time"),
      c("distance"),
      c("start_pos_lat"),
      c("start_pos_long"),
      c("calories"),
      c("total_ascend"),
      c("total_descend"),
      c("min_temp"),
      c("max_temp"),
      c("avg_temp"),
      c("min_hr"),
      c("max_hr"),
      c("avg_hr"),
      c("max_speed"),
      c("avg_speed"),
      c("max_power"),
      c("avg_power"),
      c("norm_power"),
      c("max_cadence"),
      c("avg_cadence"),
      c("tss"),
      c("num_pool_len"),
      c("iff"),
      c("swim_stroke"),
      c("avg_stroke_distance"),
      c("avg_stroke_count"),
      c("pool_length"),
      c("avg_grade")
    )

  def insert(r: ActivitySession): ConnectionIO[ActivitySessionId] =
    (fr"INSERT INTO $table ($columns) VALUES(" ++
      fr"${r.activityId}, ${r.sport}, ${r.subSport}, ${r.startTime}, ${r.endTime}, ${r.movingTime}, ${r.elapsedTime}, ${r.distance}, " ++
      fr"${r.startPosition.map(_.latitude)}, ${r.startPosition.map(_.longitude)}, ${r.calories}, " ++
      fr"${r.totalAscend}, ${r.totalDescend}, ${r.minTemp}, ${r.maxTemp}, ${r.avgTemp}, ${r.minHr}, ${r.maxHr}, " ++
      fr"${r.avgHr}, ${r.maxSpeed}, ${r.avgSpeed}, ${r.maxPower}, ${r.avgPower}, ${r.normPower}," ++
      fr"${r.maxCadence}, ${r.avgCadence}, ${r.tss}, ${r.numPoolLength}, ${r.iff}, ${r.swimStroke}, ${r.avgStrokeDistance}," ++
      fr"${r.avgStrokeCount}, ${r.poolLength}, ${r.avgGrade}" ++
      fr")").update.withUniqueGeneratedKeys[ActivitySessionId]("id")

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[ActivitySession]): ConnectionIO[Int] =
      Update[ActivitySession](sql).updateMany(tags)

  def findById(id: ActivitySessionId): ConnectionIO[Option[ActivitySession]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[ActivitySession]
      .option

  def streamAll: Stream[ConnectionIO, ActivitySession] =
    sql"SELECT $columnsWithId FROM $table".query[ActivitySession].streamWithChunkSize(50)

  def findByStartTime(
      startTime: Instant,
      withinSecs: Int,
      sport: Option[Sport],
      limit: Option[Int]
  ): ConnectionIO[List[ActivitySession]] =
    val sportFrag = sport.map(s => sql"sport = $s")
    val startFrag =
      if (withinSecs <= 0) sql"start_time = $startTime".some
      else
        val lowerBound = startTime.minusSeconds(withinSecs / 2)
        val upperBound = startTime.plusSeconds(withinSecs / 2)
        sql"start_time >= $lowerBound AND start_time <= $upperBound".some
    val limitFrag = limit.map(l => sql"LIMIT $l").getOrElse(Fragment.empty)
    val cond = List(startFrag, sportFrag).flatten
      .foldSmash1(Fragment.empty, sql" AND ", Fragment.empty)

    sql"SELECT $columnsWithId FROM $table WHERE $cond $limitFrag"
      .query[ActivitySession]
      .to[List]

  def countAll =
    sql"SELECT count(*) FROM $table".query[Long].unique

  def getStartPositions(
      id: ActivityId
  ): ConnectionIO[List[(ActivitySessionId, Position)]] =
    sql"""SELECT act.id, act.start_pos_lat, act.start_pos_long
          FROM $table act
          INNER JOIN ${RActivity.table} pa ON pa.id = act.activity_id
          WHERE pa.id = $id AND act.start_pos_lat is not null AND act.start_pos_long is not null
         """
      .query[(ActivitySessionId, Position)]
      .to[List]

  /** Finds the last position in all sessions of an activity. Since this is not recorded
    * in the session of a fit file, the position of the latest record is used here.
    */
  def getEndPositions(id: ActivityId): ConnectionIO[List[(ActivitySessionId, Position)]] =
    sql"""WITH
        latest(sessionId, ts) as (
           SELECT DISTINCT
             d.activity_session_id, max(d.timestamp)
           FROM activity_session_data d
           WHERE d.position_lat is not null AND d.position_long is not null
              AND d.activity_session_id in (select id from activity_session where activity_id = $id)
           GROUP BY d.activity_session_id
         )
      SELECT d.activity_session_id, d.position_lat, d.position_long
      FROM activity_session_data d
      INNER JOIN latest l on l.sessionId = d.activity_session_id AND l.ts = d.timestamp;
      """
      .query[(ActivitySessionId, Position)]
      .to[List]

  def sportsByActivity(id: ActivityId): ConnectionIO[Set[Sport]] =
    sql"""SELECT sport FROM $table WHERE activity_id = $id"""
      .query[Sport]
      .to[Set]

  def lowestStartTime(id: ActivityId): ConnectionIO[Option[Instant]] =
    sql"""SELECT min(start_time) FROM $table WHERE activity_id = $id"""
      .query[Instant]
      .option
