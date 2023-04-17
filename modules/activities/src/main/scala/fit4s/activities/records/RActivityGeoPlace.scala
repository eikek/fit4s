package fit4s.activities.records

import fit4s.activities.data._
import fit4s.activities.records.DoobieImplicits._
import fit4s.data.Distance

import doobie._
import doobie.implicits._

final case class RActivityGeoPlace(
    id: ActivityGeoPlaceId,
    activityId: ActivitySessionId,
    placeId: GeoPlaceId,
    name: PositionName
)

object RActivityGeoPlace {
  private[activities] val table = fr"activity_geo_place"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("activity_session_id"),
      c("geo_place_id"),
      c("position_name")
    )
  }

  private val colsNoId = columnList(None).tail.commas

  def insert(r: RActivityGeoPlace): ConnectionIO[ActivityGeoPlaceId] =
    sql"INSERT INTO $table ($colsNoId) VALUES (${r.activityId}, ${r.placeId}, ${r.name})".update
      .withUniqueGeneratedKeys[ActivityGeoPlaceId]("id")

  def insert(
      activityId: ActivitySessionId,
      placeId: GeoPlaceId,
      name: PositionName
  ): ConnectionIO[ActivityGeoPlaceId] =
    insert(RActivityGeoPlace(ActivityGeoPlaceId(-1), activityId, placeId, name))

  def delete(
      sessionId: ActivitySessionId,
      name: Option[PositionName]
  ): ConnectionIO[Int] =
    (fr"DELETE FROM $table WHERE activity_session_id = $sessionId" ++ name
      .map(n => sql"AND position_name = $n")
      .getOrElse(Fragment.empty)).update.run

  def exists(id: ActivitySessionId): ConnectionIO[Boolean] =
    sql"SELECT count(*) FROM $table WHERE activity_id = $id"
      .query[Int]
      .unique
      .map(_ > 0)

  def findByActivity(id: ActivityId): ConnectionIO[List[RActivityGeoPlace]] = {
    val c = columnList(Some("gp")).commas
    sql"""SELECT $c FROM $table gp
          INNER JOIN ${RActivitySession.table} act
             ON act.id = gp.activity_session_id
          WHERE activity_id = $id"""
      .query[RActivityGeoPlace]
      .to[List]
  }

  def getStartEndDistance(
      sessionId: ActivitySessionId
  ): ConnectionIO[Option[Distance]] = {
    val start = PositionName.Start.widen
    val end = PositionName.End.widen
    sql"""WITH
       data(session_id, position_name,position_lat,position_lng) as (
          SELECT agp.activity_session_id, agp.position_name,
              gp.position_lat, gp.position_lng
          FROM activity_geo_place agp
          INNER JOIN geo_place gp ON gp.id = agp.geo_place_id
          WHERE activity_session_id = $sessionId
             AND gp.position_lng is not null
             AND gp.position_lat is not null
       )
       SELECT
         HAVSC(
              (select position_lat from "data" where position_name = $start),
              (select position_lng from "data" where position_name = $start),
              (select position_lat from "data" where position_name = $end),
              (select position_lng from "data" where position_name = $end)
         )
      """
      .query[Option[Double]]
      .option
      .map(_.flatten.map(Distance.km))
  }
}
