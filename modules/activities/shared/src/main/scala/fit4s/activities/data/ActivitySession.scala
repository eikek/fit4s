package fit4s.activities.data

import java.time.{Duration, Instant}

import fit4s.data.*
import fit4s.profile.types.*

final case class ActivitySession(
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
    maxCadence: Option[Cadence],
    avgCadence: Option[Cadence],
    tss: Option[TrainingStressScore],
    numPoolLength: Option[Int],
    iff: Option[IntensityFactor],
    swimStroke: Option[SwimStroke],
    avgStrokeDistance: Option[Distance],
    avgStrokeCount: Option[StrokesPerLap],
    poolLength: Option[Distance],
    avgGrade: Option[Percent]
)
