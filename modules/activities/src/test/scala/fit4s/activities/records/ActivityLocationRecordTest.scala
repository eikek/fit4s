package fit4s.activities.records

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.data.LocationId
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fs2.Chunk
import fs2.io.file.Path

class ActivityLocationRecordTest extends DatabaseTest {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      val location = Path("/home/user/test")
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        record <- ActivityLocationRecord.insert(location).transact(xa)
        _ = assertEquals(record.id, LocationId(1L))
        _ = assertEquals(record.location, location)

        found <- ActivityLocationRecord.find(location).transact(xa)
        _ = assertEquals(found, Some(record))

        exists <- ActivityLocationRecord.exists(location).transact(xa)
        _ = assert(exists)
      } yield ()
    }
  }

  test("insert many and list") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- sql"DELETE FROM activity_location".update.run.transact(xa)
        locations = Chunk("/home/a", "/home/b", "/home/c").map(Path.apply)
        records <- ActivityLocationRecord
          .insertAll(locations)
          .transact(xa)
          .compile
          .toVector

        all <- ActivityLocationRecord.listAll.transact(xa).compile.toVector
        _ = assertEquals(records.sortBy(_.id), all.sortBy(_.id))
      } yield ()
    }
  }
}
