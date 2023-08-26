package fit4s.activities.data

import java.time.{Duration, Instant}

import fit4s.data._
import fit4s.profile.types._

final case class ActivityLap(
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
