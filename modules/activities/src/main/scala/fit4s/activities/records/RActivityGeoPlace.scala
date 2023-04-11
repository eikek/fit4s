package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data._
import DoobieMeta._

final case class RActivityGeoPlace(
    id: ActivityGeoPlaceId,
    activityId: ActivityId,
    placeId: GeoPlaceId
)

object RActivityGeoPlace {
  private[activities] val table = fr"activity_geo_place"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("activity_id"),
      c("geo_place_id")
    )
  }

  private val colsNoId = columnList(None).tail
    .foldSmash1(Fragment.empty, sql", ", Fragment.empty)
  private val cols = columnList(None)
    .foldSmash1(Fragment.empty, sql", ", Fragment.empty)

  def insert(r: RActivityGeoPlace): ConnectionIO[ActivityGeoPlaceId] =
    sql"INSERT INTO $table ($colsNoId) VALUES (${r.activityId}, ${r.placeId})".update
      .withUniqueGeneratedKeys[ActivityGeoPlaceId]("id")

  def insert(
      activityId: ActivityId,
      placeId: GeoPlaceId
  ): ConnectionIO[ActivityGeoPlaceId] =
    insert(RActivityGeoPlace(ActivityGeoPlaceId(-1), activityId, placeId))

  def exists(id: ActivityId): ConnectionIO[Boolean] =
    sql"SELECT count(*) FROM $table WHERE activity_id = $id"
      .query[Int]
      .unique
      .map(_ > 0)

  def findByActivity(id: ActivityId): ConnectionIO[List[RActivityGeoPlace]] =
    sql"SELECT $cols FROM $table WHERE activity_id = $id"
      .query[RActivityGeoPlace]
      .to[List]
}
