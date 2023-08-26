package fit4s.activities.data

import cats.data.NonEmptyList

import fit4s.data.Distance
import fit4s.strava.data.StravaActivityId

final case class ActivityDetailResult(
    activity: Activity,
    location: Location,
    sessions: NonEmptyList[ActivitySession],
    sessionData: Map[ActivitySessionId, List[ActivitySessionData]],
    tags: Vector[Tag],
    stravaId: Option[StravaActivityId],
    laps: Map[ActivitySessionId, List[ActivityLap]],
    startPlace: Map[ActivitySessionId, GeoPlace],
    endPlace: Map[ActivitySessionId, GeoPlace],
    startEndDistance: Map[ActivitySessionId, Distance]
)
