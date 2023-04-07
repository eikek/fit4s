package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{ActivityId, ActivitySessionId}
import fit4s.activities.records.DoobieMeta._
import fit4s.data._
import fit4s.profile.types._

import java.time.{Duration, Instant}

final case class ActivitySessionRecord(
    id: ActivitySessionId,
    activityId: ActivityId,
    sport: Sport,
    subSport: SubSport,
    startTime: Instant,
    endTime: Instant,
    movingTime: Duration,
    elapsedTime: Duration,
    distance: Distance,
    startPosition: Option[Position],
    calories: Calories,
    totalAscend: Option[Distance],
    totalDescend: Option[Distance],
    minTemp: Option[Temperature],
    maxTemp: Option[Temperature],
    avgTemp: Option[Temperature],
    minHr: Option[HeartRate],
    maxHr: Option[HeartRate],
    avgHr: Option[HeartRate],
    maxSpeed: Option[Speed],
    avgSpeed: Option[Speed],
    maxPower: Option[Power],
    avgPower: Option[Power],
    normPower: Option[Power],
    tss: Option[TrainingStressScore],
    numPoolLength: Option[Int],
    iff: Option[IntensityFactor],
    swimStroke: Option[SwimStroke],
    avgStrokeDistance: Option[Distance],
    avgStrokeCount: Option[StrokesPerLap],
    poolLength: Option[Distance],
    avgGrade: Option[Percent]
)

object ActivitySessionRecord {
  private[activities] val table = fr"activity_session"
  private val columns = columnList(None).tail
    .foldSmash1(Fragment.empty, sql", ", Fragment.empty)

  private val columnsWithId =
    columnList(None).foldSmash1(Fragment.empty, sql", ", Fragment.empty)

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
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
      c("tss"),
      c("num_pool_len"),
      c("iff"),
      c("swim_stroke"),
      c("avg_stroke_distance"),
      c("avg_stroke_count"),
      c("pool_length"),
      c("avg_grade")
    )
  }

  def insert(r: ActivitySessionRecord): ConnectionIO[ActivitySessionId] =
    (fr"INSERT INTO $table ($columns) VALUES(" ++
      fr"${r.activityId}, ${r.sport}, ${r.subSport}, ${r.startTime}, ${r.endTime}, ${r.movingTime}, ${r.elapsedTime}, ${r.distance}, " ++
      fr"${r.startPosition.map(_.latitude)}, ${r.startPosition.map(_.longitude)}, ${r.calories}, " ++
      fr"${r.totalAscend}, ${r.totalDescend}, ${r.minTemp}, ${r.maxTemp}, ${r.avgTemp}, ${r.minHr}, ${r.maxHr}, " ++
      fr"${r.avgHr}, ${r.maxSpeed}, ${r.avgSpeed}, ${r.maxPower}, ${r.avgPower}, ${r.normPower}," ++
      fr"${r.tss}, ${r.numPoolLength}, ${r.iff}, ${r.swimStroke}, ${r.avgStrokeDistance}," ++
      fr"${r.avgStrokeCount}, ${r.poolLength}, ${r.avgGrade}" ++
      fr")").update.withUniqueGeneratedKeys[ActivitySessionId]("id")

  def findById(id: ActivitySessionId): ConnectionIO[Option[ActivitySessionRecord]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[ActivitySessionRecord]
      .option

  def countAll =
    sql"SELECT count(*) FROM $table".query[Long].unique
}
