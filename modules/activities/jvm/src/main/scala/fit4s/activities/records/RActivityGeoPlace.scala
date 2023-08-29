package fit4s.activities.records

import fs2.Stream

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
  private val colsWithId = columnList(None).commas

  def streamAll: Stream[ConnectionIO, RActivityGeoPlace] =
    sql"SELECT $colsWithId FROM $table".query[RActivityGeoPlace].streamWithChunkSize(100)

  object insertMany {
    val cols = columnList(None).map(_.internals.sql).mkString(",")
    val ph = columnList(None).map(_ => "?").mkString(",")
    val tn = table.internals.sql
    val sql = s"INSERT INTO $tn ($cols) values ($ph)"

    def apply(tags: Seq[RActivityGeoPlace]): ConnectionIO[Int] =
      Update[RActivityGeoPlace](sql).updateMany(tags)
  }

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
          WHERE act.activity_id = $id"""
      .query[RActivityGeoPlace]
      .to[List]
  }

  def findMissingActivities(ids: List[ActivityId]): Stream[ConnectionIO, ActivityId] = {
    val cond = ids match {
      case Nil => Fragment.empty
      case nn =>
        fr"AND act.activity_id IN (${nn.map(_.id.toString).map(Fragment.const(_)).commas})"
    }
    sql"""WITH
            gstart as (select * from $table where position_name = 'start'),
            gend as (select * from $table where position_name = 'end')
          select distinct s.activity_id
          from ${RActivitySession.table} s
          where (s.start_pos_lat is not null AND s.start_pos_long is not null)
            AND (s.id not in (select activity_session_id from gstart)
                 OR s.id not in (select activity_session_id from gend)) $cond"""
      .query[ActivityId]
      .streamWithChunkSize(100)
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
