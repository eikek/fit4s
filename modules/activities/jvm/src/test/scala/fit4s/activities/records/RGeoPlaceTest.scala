package fit4s.activities.records

import cats.effect.IO
import cats.syntax.all._

import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fit4s.data._
import fit4s.geocode.data.BoundingBox

import doobie.implicits._

class RGeoPlaceTest extends DatabaseTest with TestData {
  override def munitFixtures = List(h2DataSource)

  test("test not in bounding box") {
    // sometimes the position is not completely in the reported bbox
    val place =
      testPlace1.copy(
        boundingBox = BoundingBox(
          Semicircle.degree(55),
          Semicircle.degree(56),
          Semicircle.degree(-3),
          Semicircle.degree(-2)
        )
      )

    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
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
        _ <- deleteAllData(xa)
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
