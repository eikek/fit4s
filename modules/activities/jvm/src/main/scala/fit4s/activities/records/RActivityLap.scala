package fit4s.activities.records

import scala.collection.immutable.Seq

import fs2.Stream

import fit4s.activities.data.{ActivityLap, ActivityLapId}
import fit4s.activities.records.DoobieImplicits.*

import doobie.*
import doobie.implicits.*

object RActivityLap:
  private[activities] val table = fr"activity_lap"
  private val columns = columnList(None).tail.commas
  private val columnsWithId = columnList(None).commas

  private[activities] def columnList(alias: Option[String]): List[Fragment] =
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("activity_session_id"),
      c("sport"),
      c("sub_sport"),
      c("trigger"),
      c("start_time"),
      c("end_time"),
      c("start_pos_lat"),
      c("start_pos_long"),
      c("end_pos_lat"),
      c("end_pos_long"),
      c("moving_time"),
      c("elapsed_time"),
      c("calories"),
      c("distance"),
      c("min_temp"),
      c("max_temp"),
      c("avg_temp"),
      c("max_speed"),
      c("avg_speed"),
      c("min_hr"),
      c("max_hr"),
      c("avg_hr"),
      c("max_power"),
      c("avg_power"),
      c("norm_power"),
      c("max_cadence"),
      c("avg_cadence"),
      c("total_ascend"),
      c("total_descend"),
      c("num_pool_len"),
      c("swim_stroke"),
      c("avg_stroke_distance"),
      c("stroke_count"),
      c("avg_grade")
    )

  def insert(r: ActivityLap): ConnectionIO[ActivityLapId] =
    (fr"INSERT INTO $table ($columns) VALUES(" ++
      fr"${r.activitySessionId}, ${r.sport}, ${r.subSport}, ${r.trigger}, ${r.startTime}, ${r.endTime}, " ++
      fr"${r.startPosition.map(_.latitude)}, ${r.startPosition.map(_.longitude)}, " ++
      fr"${r.endPosition.map(_.latitude)}, ${r.endPosition.map(_.longitude)}, " ++
      fr"${r.movingTime}, ${r.elapsedTime}, ${r.calories}, ${r.distance}, " ++
      fr"${r.minTemp}, ${r.maxTemp}, ${r.avgTemp}, ${r.maxSpeed}, ${r.avgSpeed}," ++
      fr"${r.minHr}, ${r.maxHr}, ${r.avgHr}, ${r.maxPower}, ${r.avgPower}, ${r.normPower}," ++
      fr"${r.maxCadence}, ${r.avgCadence}, ${r.totalAscend}, ${r.totalDescend}," ++
      fr"${r.numPoolLength}, ${r.swimStroke}, ${r.avgStrokeDistance}, ${r.strokeCount}," ++
      fr"${r.avgGrade}" ++
      fr")").update.withUniqueGeneratedKeys[ActivityLapId]("id")

  object insertMany:
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[ActivityLap]): ConnectionIO[Int] =
      Update[ActivityLap](sql).updateMany(tags)

  def findById(id: ActivityLapId): ConnectionIO[Option[ActivityLap]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[ActivityLap]
      .option

  def countAll =
    sql"SELECT count(*) FROM $table".query[Long].unique

  def streamAll: Stream[ConnectionIO, ActivityLap] =
    sql"SELECT $columnsWithId FROM $table".query[ActivityLap].streamWithChunkSize(50)
