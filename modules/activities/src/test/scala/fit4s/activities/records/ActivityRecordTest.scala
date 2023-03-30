package fit4s.activities.records

import cats.effect.IO
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fs2.io.file.Path
import doobie.implicits._

class ActivityRecordTest extends DatabaseTest with TestData {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
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

}
