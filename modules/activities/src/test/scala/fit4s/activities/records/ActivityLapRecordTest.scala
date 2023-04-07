package fit4s.activities.records

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fs2.io.file.Path

class ActivityLapRecordTest extends DatabaseTest with TestData {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- ActivityLocationRecord.insert(Path("/home/user/fit")).transact(xa)
        activityId <- ActivityRecord
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        sessionId <- ActivitySessionRecord
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)
        lapId <- ActivityLapRecord
          .insert(testActivityLap.copy(activitySessionId = sessionId))
          .transact(xa)
        found <- ActivityLapRecord.findById(lapId).transact(xa)
        expect = testActivityLap.copy(id = lapId, activitySessionId = sessionId)
        _ = assertEquals(found, Some(expect))
      } yield ()
    }
  }
}
