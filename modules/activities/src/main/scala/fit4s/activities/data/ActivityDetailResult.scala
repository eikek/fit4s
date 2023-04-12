package fit4s.activities.data

import cats.data.NonEmptyList
import fit4s.activities.records._
import fit4s.data.Distance

final case class ActivityDetailResult(
    activity: RActivity,
    location: RActivityLocation,
    sessions: NonEmptyList[RActivitySession],
    tags: Vector[RTag],
    stravaId: Option[StravaExternalId],
    startPlace: Map[ActivitySessionId, RGeoPlace],
    endPlace: Map[ActivitySessionId, RGeoPlace],
    startEndDistance: Map[ActivitySessionId, Distance]
)
