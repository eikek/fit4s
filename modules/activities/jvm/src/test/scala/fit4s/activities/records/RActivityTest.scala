package fit4s.activities.records

import cats.effect.IO
import fs2.io.file.Path

import fit4s.activities.{DatabaseTest, FlywayMigrate}

import doobie.implicits.*

class RActivityTest extends DatabaseTest with TestData:
  override def munitFixtures = List(h2DataSource)

  test("insert record"):
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- RActivityLocation.insert(Path("/home/user/fit")).transact(xa)
        recordId <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        found <- RActivity.findById(recordId).transact(xa)
        expect = testActivity.copy(id = recordId, locationId = location.id)
        _ = assertEquals(found, Some(expect))
      } yield ()
    }

  test("fail on duplicate file-ids"):
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- RActivityLocation.insert(Path("/home/user/fit")).transact(xa)
        _ <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        result <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
          .attempt
        _ = assert(result.isLeft)
      } yield ()
    }
