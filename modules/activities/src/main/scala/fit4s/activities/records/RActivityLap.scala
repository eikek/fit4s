package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{ActivityLapId, ActivitySessionId}
import fit4s.activities.records.DoobieImplicits._
import fit4s.data._
import fit4s.profile.types._

import java.time.{Duration, Instant}

final case class RActivityLap(
    id: ActivityLapId,
    activitySessionId: ActivitySessionId,
    sport: Sport,
    subSport: SubSport,
    trigger: Option[LapTrigger],
    startTime: Instant,
    endTime: Instant,
    startPosition: Option[Position],
    endPosition: Option[Position],
    movingTime: Option[Duration],
    elapsedTime: Option[Duration],
    calories: Calories,
    distance: Distance,
    minTemp: Option[Temperature],
    maxTemp: Option[Temperature],
    avgTemp: Option[Temperature],
    maxSpeed: Option[Speed],
    avgSpeed: Option[Speed],
    minHr: Option[HeartRate],
    maxHr: Option[HeartRate],
    avgHr: Option[HeartRate],
    maxPower: Option[Power],
    avgPower: Option[Power],
    normPower: Option[Power],
    maxCadence: Option[Cadence],
    avgCadence: Option[Cadence],
    totalAscend: Option[Distance],
    totalDescend: Option[Distance],
    numPoolLength: Option[Int],
    swimStroke: Option[SwimStroke],
    avgStrokeDistance: Option[Distance],
    strokeCount: Option[Int],
    avgGrade: Option[Percent]
)

object RActivityLap {
  private[activities] val table = fr"activity_lap"
  private val columns = columnList(None).tail.commas
  private val columnsWithId = columnList(None).commas

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
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
  }

  def insert(r: RActivityLap): ConnectionIO[ActivityLapId] =
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

  def findById(id: ActivityLapId): ConnectionIO[Option[RActivityLap]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[RActivityLap]
      .option

  def countAll =
    sql"SELECT count(*) FROM $table".query[Long].unique
}
