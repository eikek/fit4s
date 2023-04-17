package fit4s.activities.records

import cats.syntax.all._

import fit4s.activities.data.{CountryCode, GeoPlaceId, PostCode}
import fit4s.activities.records.DoobieImplicits._
import fit4s.data.Position
import fit4s.geocode._

import doobie._
import doobie.implicits._

final case class RGeoPlace(
    id: GeoPlaceId,
    osmPlaceId: NominatimPlaceId,
    osmId: NominatimOsmId,
    position: Position,
    road: Option[String],
    location: String,
    country: Option[String],
    countryCode: CountryCode,
    postCode: Option[PostCode],
    boundingBox: BoundingBox
)

object RGeoPlace {
  def fromPlace(id: GeoPlaceId, place: Place): Option[RGeoPlace] =
    (
      place.address.city
        .orElse(place.address.village),
      place.address.country_code.map(CountryCode.apply)
    )
      .mapN { (loc, cc) =>
        RGeoPlace(
          id = id,
          osmPlaceId = place.place_id,
          osmId = place.osm_id,
          position = place.position,
          road = place.address.road,
          location = loc,
          country = place.address.country,
          countryCode = cc,
          postCode = place.address.postcode.map(PostCode.apply),
          boundingBox = place.boundingbox
        )
      }

  private[activities] val table = fr"geo_place"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String) = Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(
      c("id"),
      c("osm_place_id"),
      c("osm_id"),
      c("position_lat"),
      c("position_lng"),
      c("road"),
      c("location"),
      c("country"),
      c("country_code"),
      c("post_code"),
      c("bbox_lat1"),
      c("bbox_lat2"),
      c("bbox_lng1"),
      c("bbox_lng2")
    )
  }

  private val colsNoId = columnList(None).tail.commas
  private val cols = columnList(None).commas

  def insert(r: RGeoPlace): ConnectionIO[GeoPlaceId] =
    (sql"INSERT INTO $table ($colsNoId) VALUES (" ++
      sql"${r.osmPlaceId}, ${r.osmId}, ${r.position.latitude}, ${r.position.longitude}, " ++
      sql"${r.road}, ${r.location}, ${r.country}, ${r.countryCode}, ${r.postCode}, " ++
      sql"${r.boundingBox.lat1}, ${r.boundingBox.lat2}, ${r.boundingBox.lng1}, ${r.boundingBox.lng2}" ++
      sql")").update.withUniqueGeneratedKeys[GeoPlaceId]("id")

  def findByPosition(pos: Position): ConnectionIO[Option[(RGeoPlace, Double)]] =
    // find in bbox order by distance-to-position asc
    sql"""SELECT $cols, HAVSC(${pos.latitude}, ${pos.longitude}, position_lat, position_lng) as dst FROM $table
          WHERE (ABS(position_lat - ${pos.latitude}) < 800 AND ABS(position_lng - ${pos.longitude}) < 800) OR
               (bbox_lat1 - 500 <= ${pos.latitude} AND
                bbox_lat2 + 500 >= ${pos.latitude} AND
                bbox_lng1 - 500 <= ${pos.longitude} AND
                bbox_lng2 + 500 >= ${pos.longitude})
          ORDER BY dst ASC
          LIMIT 1
          """.query[(RGeoPlace, Double)].option

  def findByPlace(p: Place): ConnectionIO[Option[RGeoPlace]] =
    sql"""SELECT $cols FROM $table
         WHERE (position_lat = ${p.lat} AND position_lng = ${p.lon})
           OR osm_place_id = ${p.place_id}
         LIMIT 1"""
      .query[RGeoPlace]
      .option
}
