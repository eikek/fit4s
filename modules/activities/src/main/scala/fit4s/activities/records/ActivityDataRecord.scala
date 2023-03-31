package fit4s.activities.records

import fit4s.data._
import doobie.implicits._
import fit4s.activities.data.{ActivityDataId, ActivityId}

import java.time.Instant

final case class ActivityDataRecord(
    id: ActivityDataId,
    activityId: ActivityId,
    timestamp: Instant,
    positionLat: Option[Semicircle],
    positionLong: Option[Semicircle],
    altitude: Option[Distance],
    heartRate: Option[HeartRate],
    cadence: Option[Cadence],
    distance: Option[Distance],
    speed: Option[Speed],
    power: Option[Power],
    grade: Option[Grade],
    temperature: Option[Temperature],
    calories: Option[Calories]
)

@annotation.nowarn
object ActivityDataRecord {
  private val table = fr"activity_data"
}
