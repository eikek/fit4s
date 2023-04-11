package fit4s.activities.records

import cats.effect.IO
import cats.syntax.all._
import doobie.implicits._
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fit4s.activities.data.{CountryCode, GeoPlaceId}
import fit4s.data._
import fit4s.geocode.{BoundingBox, NominatimOsmId, NominatimPlaceId}

class RGeoPlaceTest extends DatabaseTest {
  override def munitFixtures = List(h2DataSource)

  val place1 = RGeoPlace(
    id = GeoPlaceId(-1),
    osmPlaceId = NominatimPlaceId(15),
    osmId = NominatimOsmId(56),
    position = Position(Semicircle.degree(51.5), Semicircle.degree(0)),
    road = None,
    location = "London",
    country = None,
    countryCode = CountryCode("gb"),
    postCode = None,
    boundingBox = BoundingBox(
      Semicircle.degree(50),
      Semicircle.degree(52),
      Semicircle.degree(-1),
      Semicircle.degree(1)
    )
  )

  val place2 = RGeoPlace(
    id = GeoPlaceId(-1),
    osmPlaceId = NominatimPlaceId(16),
    osmId = NominatimOsmId(58),
    position = Position(Semicircle.degree(50.8), Semicircle.degree(0.4)),
    road = None,
    location = "London?",
    country = None,
    countryCode = CountryCode("gb"),
    postCode = None,
    boundingBox = BoundingBox(
      Semicircle.degree(50),
      Semicircle.degree(52),
      Semicircle.degree(-1),
      Semicircle.degree(1)
    )
  )

  val place3 = RGeoPlace(
    id = GeoPlaceId(-1),
    osmPlaceId = NominatimPlaceId(30),
    osmId = NominatimOsmId(151),
    position = Position(Semicircle.degree(38.8), Semicircle.degree(-77.1)),
    road = None,
    location = "Arlington",
    country = None,
    countryCode = CountryCode("us"),
    postCode = None,
    boundingBox = BoundingBox(
      Semicircle.degree(37),
      Semicircle.degree(39),
      Semicircle.degree(-77.5),
      Semicircle.degree(78)
    )
  )

  test("test distance") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- List(place1, place2, place3)
          .traverse_(RGeoPlace.insert)
          .transact(xa)

        res1 <- RGeoPlace
          .findPosition(Position(Semicircle.degree(50.6), Semicircle.degree(0.5)))
          .transact(xa)
        _ = assertEquals(res1.get._1, place2.copy(id = res1.get._1.id))

        res3 <- RGeoPlace
          .findPosition(place3.position)
          .transact(xa)
        _ = assertEquals(res3.get._1, place3.copy(id = res3.get._1.id))
        _ = assertEquals(res3.get._2, 0d)

        res2 <- RGeoPlace
          .findPosition(Position(Semicircle.degree(12), Semicircle.degree(-6)))
          .transact(xa)
        _ = assertEquals(res2, None)
      } yield ()
    }
  }
}
