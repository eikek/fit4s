package fit4s.activities.data

import fit4s.data._
import fit4s.profile.types._

import java.time.{Duration, Instant}

final case class ActivitySessionSummary(
    sport: Sport,
    startTime: Instant,
    endTime: Instant,
    movingTime: Duration,
    elapsedTime: Duration,
    distance: Distance,
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
    avgNormPower: Option[Power],
    avgGrade: Option[Percent],
    avgIntensity: Option[IntensityFactor],
    avgTss: Option[TrainingStressScore],
    count: Int
)
