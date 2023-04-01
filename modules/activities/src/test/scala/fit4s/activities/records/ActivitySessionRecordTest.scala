package fit4s.activities.records

import cats.effect.IO
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fs2.io.file.Path
import doobie.implicits._

class ActivitySessionRecordTest extends DatabaseTest with TestData {
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
        recordId <- ActivitySessionRecord
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)
        found <- ActivitySessionRecord.findById(recordId).transact(xa)
        expect = testActivitySession.copy(id = recordId, activityId = activityId)
        _ = assertEquals(found, Some(expect))
      } yield ()
    }
  }

}
