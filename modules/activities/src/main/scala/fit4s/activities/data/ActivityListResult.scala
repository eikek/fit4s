package fit4s.activities.data

import cats.data.NonEmptyList

import fit4s.activities.records._

final case class ActivityListResult(
    activity: RActivity,
    location: RActivityLocation,
    sessions: NonEmptyList[RActivitySession],
    tags: Vector[RTag]
)
