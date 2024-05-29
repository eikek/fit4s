package fit4s.activities.data

import cats.data.NonEmptyList

import fit4s.strava.data.StravaActivityId

final case class ActivityListResult(
    activity: Activity,
    stravaId: Option[StravaActivityId],
    location: Location,
    sessions: NonEmptyList[ActivitySession],
    tags: Vector[Tag]
):

  lazy val id: ActivityId = activity.id
