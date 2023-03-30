package fit4s.activities.records

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.{DatabaseTest, FlywayMigrate}

class TagRecordTest extends DatabaseTest {
  override def munitFixtures = List(h2DataSource)

  test("insert record") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        record <- TagRecord.insert("atag").transact(xa)
        _ = assertEquals(record.id, 1L)
        _ = assertEquals(record.name, "atag")

        found <- TagRecord.find("atag").transact(xa)
        _ = assertEquals(found, Some(record))

        exists <- TagRecord.exists("atag").transact(xa)
        _ = assert(exists)
      } yield ()
    }
  }
}
