package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data._
import DoobieMeta._

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

  private val colsNoId = columnList(None).tail
    .foldSmash1(Fragment.empty, sql", ", Fragment.empty)

  def insert(r: RActivityGeoPlace): ConnectionIO[ActivityGeoPlaceId] =
    sql"INSERT INTO $table ($colsNoId) VALUES (${r.activityId}, ${r.placeId}, ${r.name})".update
      .withUniqueGeneratedKeys[ActivityGeoPlaceId]("id")

  def insert(
      activityId: ActivitySessionId,
      placeId: GeoPlaceId,
      name: PositionName
  ): ConnectionIO[ActivityGeoPlaceId] =
    insert(RActivityGeoPlace(ActivityGeoPlaceId(-1), activityId, placeId, name))

  def exists(id: ActivitySessionId): ConnectionIO[Boolean] =
    sql"SELECT count(*) FROM $table WHERE activity_id = $id"
      .query[Int]
      .unique
      .map(_ > 0)

  def findByActivity(id: ActivityId): ConnectionIO[List[RActivityGeoPlace]] = {
    val c = columnList(Some("gp")).foldSmash1(Fragment.empty, sql",", Fragment.empty)
    sql"""SELECT $c FROM $table gp
          INNER JOIN ${RActivitySession.table} act
             ON act.id = gp.activity_session_id
          WHERE activity_id = $id"""
      .query[RActivityGeoPlace]
      .to[List]
  }
}
