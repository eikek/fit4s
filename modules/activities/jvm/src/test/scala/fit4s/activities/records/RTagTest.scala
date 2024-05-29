package fit4s.activities.records

import cats.effect.IO

import fit4s.activities.data.{TagId, TagName}
import fit4s.activities.{DatabaseTest, FlywayMigrate}

import doobie.implicits._

class RTagTest extends DatabaseTest:
  override def munitFixtures = List(h2DataSource)

  test("insert record"):
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        tag = TagName.unsafeFromString("a-tag")
        record <- RTag.insert(tag).transact(xa)
        _ = assertEquals(record.id, TagId(1L))
        _ = assertEquals(record.name, tag)

        found <- RTag.find(tag).transact(xa)
        _ = assertEquals(found, Some(record))

        exists <- RTag.exists(tag).transact(xa)
        _ = assert(exists)
      } yield ()
    }
