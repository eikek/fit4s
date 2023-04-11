package fit4s.activities.records

import cats.effect.IO
import cats.syntax.all._
import doobie.implicits._
import fit4s.activities.data.{CountryCode, GeoPlaceId, PostCode}
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fit4s.data._
import fit4s.geocode.{BoundingBox, NominatimOsmId, NominatimPlaceId}

class RGeoPlaceTest extends DatabaseTest with TestData {
  override def munitFixtures = List(h2DataSource)

  // TODO better data not leak
  test("test not in bounding box") {
    // sometimes the position is not completely in the reported bbox
    val place =
      RGeoPlace(
        id = GeoPlaceId(-1),
        osmPlaceId = NominatimPlaceId(94115511),
        osmId = NominatimOsmId(9056605070L),
        position =
          Position(Semicircle.semicircle(566560625), Semicircle.semicircle(104581107)),
        road = Some("Arbergstrasse"),
        location = "Winterthur",
        country = Some("Schweiz/Suisse"),
        countryCode = CountryCode("ch"),
        postCode = Some(PostCode("8405")),
        boundingBox = BoundingBox(
          Semicircle.semicircle(566560028),
          Semicircle.semicircle(566561221),
          Semicircle.semicircle(104544334),
          Semicircle.semicircle(104551727)
        )
      )

    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- List(place)
          .traverse_(RGeoPlace.insert)
          .transact(xa)

        res1 <- RGeoPlace
          .findByPosition(place.position)
          .transact(xa)
        _ <- IO.println(res1)
        _ = assertEquals(res1.get._1, place.copy(id = res1.get._1.id))
      } yield ()
    }
  }

  test("test distance") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- List(testPlace1, testPlace2, testPlace3)
          .traverse_(RGeoPlace.insert)
          .transact(xa)

        res1 <- RGeoPlace
          .findByPosition(Position(Semicircle.degree(50.6), Semicircle.degree(0.5)))
          .transact(xa)
        _ = assertEquals(res1.get._1, testPlace2.copy(id = res1.get._1.id))

        res3 <- RGeoPlace
          .findByPosition(testPlace3.position)
          .transact(xa)
        _ = assertEquals(res3.get._1, testPlace3.copy(id = res3.get._1.id))
        _ = assertEquals(res3.get._2, 0d)

        res2 <- RGeoPlace
          .findByPosition(Position(Semicircle.degree(12), Semicircle.degree(-6)))
          .transact(xa)
        _ = assertEquals(res2, None)
      } yield ()
    }
  }
}
