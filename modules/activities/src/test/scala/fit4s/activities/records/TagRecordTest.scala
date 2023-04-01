package fit4s.activities.records

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.data.{TagId, TagName}
import fit4s.activities.{DatabaseTest, FlywayMigrate}

class TagRecordTest extends DatabaseTest {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        tag = TagName.unsafeFromString("a-tag")
        record <- TagRecord.insert(tag).transact(xa)
        _ = assertEquals(record.id, TagId(1L))
        _ = assertEquals(record.name, tag)

        found <- TagRecord.find(tag).transact(xa)
        _ = assertEquals(found, Some(record))

        exists <- TagRecord.exists(tag).transact(xa)
        _ = assert(exists)
      } yield ()
    }
  }
}
