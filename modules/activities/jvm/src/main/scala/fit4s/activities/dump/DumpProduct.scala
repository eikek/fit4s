package fit4s.activities.dump

import cats.syntax.all.*

import fit4s.activities.data.*
import fit4s.activities.records.*

import doobie._

final private case class DumpProduct(
    tags: List[Tag],
    locations: List[Location],
    activities: List[Activity],
    sessions: List[ActivitySession],
    laps: List[ActivityLap],
    sessionData: List[ActivitySessionData],
    activityTags: List[RActivityTag],
    activityStrava: List[RActivityStrava],
    geoPlaces: List[GeoPlace],
    activityGeoPlaces: List[RActivityGeoPlace],
    stravaTokens: List[RStravaToken]
):

  def add(p: DumpFormat): DumpProduct = p match
    case DumpFormat.DTag(value)         => copy(tags = value :: tags)
    case DumpFormat.DLocation(value)    => copy(locations = value :: locations)
    case DumpFormat.DActivity(value)    => copy(activities = value :: activities)
    case DumpFormat.DSession(value)     => copy(sessions = value :: sessions)
    case DumpFormat.DLap(value)         => copy(laps = value :: laps)
    case DumpFormat.DSessionData(value) => copy(sessionData = value :: sessionData)
    case DumpFormat.DActivityTag(value) => copy(activityTags = value :: activityTags)
    case DumpFormat.DActivityStrava(value) =>
      copy(activityStrava = value :: activityStrava)
    case DumpFormat.DGeoPlace(value) => copy(geoPlaces = value :: geoPlaces)
    case DumpFormat.DActivityGeoPlace(value) =>
      copy(activityGeoPlaces = value :: activityGeoPlaces)
    case DumpFormat.DStravaToken(value) => copy(stravaTokens = value :: stravaTokens)

  def insertStatement: ConnectionIO[Int] = List(
    RTag.insertMany(tags.filter(_.id.id >= 0)),
    RActivityLocation.insertMany(locations),
    RActivity.insertMany(activities),
    RActivitySession.insertMany(sessions),
    RActivityLap.insertMany(laps),
    RActivitySessionData.insertMany(sessionData),
    RActivityTag.insertMany(activityTags),
    RActivityStrava.insertMany(activityStrava),
    RGeoPlace.insertMany(geoPlaces),
    RActivityGeoPlace.insertMany(activityGeoPlaces),
    RStravaToken.insertMany(stravaTokens)
  ).sequence.map(_.sum)

private object DumpProduct:
  val empty: DumpProduct = DumpProduct(
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty,
    List.empty
  )
