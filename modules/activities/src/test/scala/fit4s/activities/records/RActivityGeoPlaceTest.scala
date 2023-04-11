package fit4s.activities.records

import cats.effect.IO
import cats.syntax.all._
import doobie.implicits._
import fit4s.activities.data.PositionName
import fit4s.activities.records.TestData._
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fit4s.data.Distance
import fs2.io.file.Path

class RActivityGeoPlaceTest extends DatabaseTest {
  override def munitFixtures = List(h2DataSource)

  test("test distance") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)

        placeIds <- List(testPlace1, testPlace2)
          .traverse(RGeoPlace.insert)
          .transact(xa)

        location <- RActivityLocation.insert(Path("/home/user/fit")).transact(xa)
        activityId <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        sessionId <- RActivitySession
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)

        _ <- RActivityGeoPlace
          .insert(sessionId, placeIds.head, PositionName.Start)
          .transact(xa)
        _ <- RActivityGeoPlace
          .insert(sessionId, placeIds(1), PositionName.End)
          .transact(xa)

        dst <- RActivityGeoPlace.getStartEndDistance(sessionId).transact(xa)
        _ = assertEquals(dst.map(_.rounded), Some(Distance.meter(82685)))
      } yield ()
    }
  }

  test("test zero distance") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)

        placeId <- RGeoPlace.insert(testPlace3).transact(xa)

        location <- RActivityLocation.insert(Path("/home/user/fit3")).transact(xa)
        activityId <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        sessionId <- RActivitySession
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)

        _ <- RActivityGeoPlace
          .insert(sessionId, placeId, PositionName.Start)
          .transact(xa)
        _ <- RActivityGeoPlace
          .insert(sessionId, placeId, PositionName.End)
          .transact(xa)

        dst <- RActivityGeoPlace.getStartEndDistance(sessionId).transact(xa)
        _ = assertEquals(dst.map(_.rounded), Some(Distance.meter(0)))
      } yield ()
    }
  }
}
