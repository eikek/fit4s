package fit4s.activities.impl

import cats.data.{NonEmptyList, OptionT}
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.data.{
  ActivityDetailResult,
  ActivityId,
  ActivitySessionId,
  PositionName
}
import fit4s.activities.records._
import DoobieMeta._

object ActivityDetailQuery {
  private[this] val activityT = RActivity.table
  private[this] val locationT = RActivityLocation.table
  private[this] val tagT = RTag.table
  private[this] val actTagT = RActivityTag.table
  private[this] val sessionT = RActivitySession.table

  def create(id: ActivityId): ConnectionIO[Option[ActivityDetailResult]] =
    (for {
      al <- OptionT(activityAndLocation(id))
      tt <- OptionT.liftF(activityTags(id))
      ses <- OptionT.liftF(activitySessions(id))
      stravaId <- OptionT.liftF(RActivityStrava.find(id))
      places <- OptionT.liftF(places(id))
      dist <- OptionT.liftF(
        ses.toList.traverse(s =>
          RActivityGeoPlace.getStartEndDistance(s.id).map(_.map(s.id -> _))
        )
      )
      laps <- OptionT.liftF(
        ses.toList.traverse(s => laps(s.id).map(s.id -> _))
      )
    } yield ActivityDetailResult(
      activity = al._1,
      location = al._2,
      sessions = ses,
      tags = tt,
      stravaId = stravaId,
      laps = laps.toMap,
      startPlace =
        places.view.withFilter(_._2 == PositionName.Start).map(t => t._1 -> t._3).toMap,
      endPlace =
        places.view.withFilter(_._2 == PositionName.End).map(t => t._1 -> t._3).toMap,
      startEndDistance = dist.flatten.toMap
    )).value

  private def laps(id: ActivitySessionId): ConnectionIO[List[RActivityLap]] = {
    val cols = RActivityLap
      .columnList(Some("lap"))
      .foldSmash1(Fragment.empty, sql",", Fragment.empty)
    sql"""SELECT $cols
          FROM ${RActivityLap.table} lap
          WHERE lap.activity_session_id = $id
          ORDER BY lap.start_time ASC"""
      .query[RActivityLap]
      .to[List]
  }

  private def places(id: ActivityId) = {
    val placeCols =
      RGeoPlace.columnList(Some("p")).foldSmash1(Fragment.empty, sql",", Fragment.empty)
    sql"""SELECT a.activity_session_id, a.position_name, $placeCols
          FROM ${RActivityGeoPlace.table} a
          INNER JOIN ${RGeoPlace.table} p ON a.geo_place_id = p.id
          INNER JOIN $sessionT sa ON sa.id = a.activity_session_id
          INNER JOIN $activityT pa ON pa.id = sa.activity_id
          WHERE pa.id = $id"""
      .query[(ActivitySessionId, PositionName, RGeoPlace)]
      .to[List]
  }

  private def activitySessions(
      id: ActivityId
  ): ConnectionIO[NonEmptyList[RActivitySession]] = {
    val actCols = RActivitySession
      .columnList(Some("act"))
      .foldSmash1(Fragment.empty, sql",", Fragment.empty)
    sql"""SELECT $actCols
          FROM $sessionT act
          WHERE act.activity_id = $id
          ORDER BY start_time"""
      .query[RActivitySession]
      .to[List]
      .map(NonEmptyList.fromList)
      .flatMap {
        case Some(nel) => nel.pure[ConnectionIO]
        case None =>
          Sync[ConnectionIO].raiseError(new Exception(s"No sessions for activity: $id"))
      }
  }

  private def activityTags(id: ActivityId) = {
    val tagCols = RTag
      .columnList(Some("tag"))
      .foldSmash1(Fragment.empty, sql", ", Fragment.empty)
    sql"""SELECT DISTINCT $tagCols
          FROM $tagT tag
          INNER JOIN $actTagT at ON tag.id = at.tag_id
          WHERE at.activity_id = $id
          ORDER BY tag.name"""
      .query[RTag]
      .to[Vector]
  }

  private def activityAndLocation(id: ActivityId) = {
    val colsAT =
      RActivity.columnList(Some("at")).foldSmash1(Fragment.empty, sql",", Fragment.empty)
    val colsLoc = RActivityLocation
      .columnList(Some("loc"))
      .foldSmash1(Fragment.empty, sql",", Fragment.empty)

    sql"""SELECT $colsAT, $colsLoc
          FROM $activityT at
          INNER JOIN $locationT loc ON loc.id = at.location_id
          WHERE at.id = $id"""
      .query[(RActivity, RActivityLocation)]
      .option
  }
}
