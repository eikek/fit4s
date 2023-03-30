package fit4s.activities.records

import fit4s.data.{Calories, Distance, FileId, HeartRate, Power, Speed, Temperature}
import fit4s.profile.types._

import java.time.{Duration, Instant}

final case class ActivityRecord(
    id: Long,
    locationId: Long,
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
    avgPower: Option[Power]
)
