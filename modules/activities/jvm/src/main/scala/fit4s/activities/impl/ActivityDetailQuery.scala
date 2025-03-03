package fit4s.activities.impl

import cats.data.{NonEmptyList, OptionT}
import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.data.*
import fit4s.activities.records.*
import fit4s.activities.records.DoobieImplicits.{*, given}

import doobie.*
import doobie.syntax.all.*

object ActivityDetailQuery:
  private val activityT = RActivity.table
  private val locationT = RActivityLocation.table
  private val tagT = RTag.table
  private val actTagT = RActivityTag.table
  private val sessionT = RActivitySession.table

  def create(
      id: ActivityId,
      withSessionData: Boolean
  ): ConnectionIO[Option[ActivityDetailResult]] =
    (for {
      al <- OptionT(activityAndLocation(id))
      tt <- OptionT.liftF(activityTags(id))
      ses <- OptionT.liftF(activitySessions(id))
      stravaId <- OptionT.liftF(RActivityStrava.findByActivityId(id))
      places <- OptionT.liftF(places(id))
      dist <- OptionT.liftF(
        ses.toList.traverse(s =>
          RActivityGeoPlace.getStartEndDistance(s.id).map(_.map(s.id -> _))
        )
      )
      laps <- OptionT.liftF(
        ses.toList.traverse(s => laps(s.id).map(s.id -> _))
      )
      sessionData <- OptionT
        .whenF(withSessionData)(
          ses.toList
            .traverse(s => RActivitySessionData.findForSession(s.id).map(s.id -> _))
        )
        .orElse(OptionT.some(Nil))
    } yield ActivityDetailResult(
      activity = al._1,
      location = al._2,
      sessions = ses,
      sessionData = sessionData.toMap,
      tags = tt,
      stravaId = stravaId,
      laps = laps.toMap,
      startPlace =
        places.view.withFilter(_._2 == PositionName.Start).map(t => t._1 -> t._3).toMap,
      endPlace =
        places.view.withFilter(_._2 == PositionName.End).map(t => t._1 -> t._3).toMap,
      startEndDistance = dist.flatten.toMap
    )).value

  private def laps(id: ActivitySessionId): ConnectionIO[List[ActivityLap]] =
    val cols = RActivityLap
      .columnList(Some("lap"))
      .commas
    sql"""SELECT $cols
          FROM ${RActivityLap.table} lap
          WHERE lap.activity_session_id = $id
          ORDER BY lap.start_time ASC"""
      .query[ActivityLap]
      .to[List]

  private def places(id: ActivityId) =
    val placeCols = RGeoPlace.columnList(Some("p")).commas
    sql"""SELECT a.activity_session_id, a.position_name, $placeCols
          FROM ${RActivityGeoPlace.table} a
          INNER JOIN ${RGeoPlace.table} p ON a.geo_place_id = p.id
          INNER JOIN $sessionT sa ON sa.id = a.activity_session_id
          INNER JOIN $activityT pa ON pa.id = sa.activity_id
          WHERE pa.id = $id"""
      .query[(ActivitySessionId, PositionName, GeoPlace)]
      .to[List]

  private def activitySessions(
      id: ActivityId
  ): ConnectionIO[NonEmptyList[ActivitySession]] =
    val actCols = RActivitySession.columnList(Some("act")).commas
    sql"""SELECT $actCols
          FROM $sessionT act
          WHERE act.activity_id = $id
          ORDER BY start_time"""
      .query[ActivitySession]
      .to[List]
      .map(NonEmptyList.fromList)
      .flatMap:
        case Some(nel) => nel.pure[ConnectionIO]
        case None =>
          Sync[ConnectionIO].raiseError(new Exception(s"No sessions for activity: $id"))

  private def activityTags(id: ActivityId) =
    val tagCols = RTag.columnList(Some("tag")).commas
    sql"""SELECT DISTINCT $tagCols
          FROM $tagT tag
          INNER JOIN $actTagT at ON tag.id = at.tag_id
          WHERE at.activity_id = $id
          ORDER BY tag.name"""
      .query[Tag]
      .to[Vector]

  private def activityAndLocation(id: ActivityId) =
    val colsAT = RActivity.columnList(Some("at")).commas
    val colsLoc = RActivityLocation.columnList(Some("loc")).commas

    sql"""SELECT $colsAT, $colsLoc
          FROM $activityT at
          INNER JOIN $locationT loc ON loc.id = at.location_id
          WHERE at.id = $id"""
      .query[(Activity, Location)]
      .option
