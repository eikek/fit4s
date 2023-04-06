package fit4s.activities.data

import cats.data.NonEmptyList
import fit4s.activities.records._

final case class ActivityListResult(
    activity: ActivityRecord,
    location: ActivityLocationRecord,
    sessions: NonEmptyList[ActivitySessionRecord],
    tags: Vector[TagRecord]
)
