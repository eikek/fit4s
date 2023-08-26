package fit4s.activities.records

import cats.effect.IO
import fs2.io.file.Path

import fit4s.activities.{DatabaseTest, FlywayMigrate}

import doobie.implicits._

class RActivityLapTest extends DatabaseTest with TestData {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- RActivityLocation.insert(Path("/home/user/fit")).transact(xa)
        activityId <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        sessionId <- RActivitySession
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)
        lapId <- RActivityLap
          .insert(testActivityLap.copy(activitySessionId = sessionId))
          .transact(xa)
        found <- RActivityLap.findById(lapId).transact(xa)
        expect = testActivityLap.copy(id = lapId, activitySessionId = sessionId)
        _ = assertEquals(found, Some(expect))
      } yield ()
    }
  }
}
