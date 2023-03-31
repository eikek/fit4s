package fit4s.activities.records

import fit4s.data.{Calories, Distance, FileId, HeartRate, Power, Speed, Temperature}
import fit4s.profile.types._
import doobie.implicits._
import doobie._
import DoobieMeta._
import fit4s.activities.data.{ActivityId, LocationId}

import java.time.{Duration, Instant}

final case class ActivityRecord(
    id: ActivityId,
    locationId: LocationId,
    path: String,
    activityFileId: FileId,
    sport: Sport,
    subSport: SubSport,
    startTime: Instant,
    movingTime: Duration,
    elapsedTime: Duration,
    distance: Distance,
    calories: Option[Calories],
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
    notes: Option[String]
)

object ActivityRecord {
  private val table = fr"activity"
  private val columns =
    fr"location_id, path, file_id, sport, sub_sport, start_time, moving_time, " ++
      fr"elapsed_time, distance, calories, min_temp, max_temp, avg_temp, min_hr, " ++
      fr"max_hr, avg_hr, max_speed, avg_speed, max_power, avg_power, notes"

  private val columnsWithId = fr"id," ++ columns

  def insert(r: ActivityRecord): ConnectionIO[Long] =
    (fr"INSERT INTO $table ($columns) VALUES(" ++
      fr"${r.locationId}, ${r.path}, ${r.activityFileId}, ${r.sport}, " ++
      fr"${r.subSport}, ${r.startTime}, ${r.movingTime}, ${r.elapsedTime}, ${r.distance}, " ++
      fr"${r.calories}, ${r.minTemp}, ${r.maxTemp}, ${r.avgTemp}, ${r.minHr}, ${r.maxHr}, " ++
      fr"${r.avgHr}, ${r.maxSpeed}, ${r.avgSpeed}, ${r.maxPower}, ${r.avgPower}, ${r.notes}" ++
      fr")").update.withUniqueGeneratedKeys[Long]("id")

  def findById(id: Long): ConnectionIO[Option[ActivityRecord]] =
    fr"SELECT $columnsWithId FROM $table WHERE id = $id"
      .query[ActivityRecord]
      .option
}
