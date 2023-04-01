package fit4s.activities.records

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fs2.io.file.Path

class ActivityRecordTest extends DatabaseTest with TestData {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- ActivityLocationRecord.insert(Path("/home/user/fit")).transact(xa)
        recordId <- ActivityRecord
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        found <- ActivityRecord.findById(recordId).transact(xa)
        expect = testActivity.copy(id = recordId, locationId = location.id)
        _ = assertEquals(found, Some(expect))
      } yield ()
    }
  }

  test("fail on duplicate file-ids") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- ActivityLocationRecord.insert(Path("/home/user/fit")).transact(xa)
        _ <- ActivityRecord
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        result <- ActivityRecord
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
          .attempt
        _ = assert(result.isLeft)
      } yield ()
    }
  }

}
