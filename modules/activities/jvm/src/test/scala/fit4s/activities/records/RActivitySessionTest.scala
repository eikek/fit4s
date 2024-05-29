package fit4s.activities.records

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.activities.{DatabaseTest, FlywayMigrate}

import doobie.implicits.*

class RActivitySessionTest extends DatabaseTest with TestData:
  override def munitFixtures = List(h2DataSource)

  test("insert record"):
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- RActivityLocation.insert(Path("/home/user/fit")).transact(xa)
        activityId <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        recordId <- RActivitySession
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)
        found <- RActivitySession.findById(recordId).transact(xa)
        expect = testActivitySession.copy(id = recordId, activityId = activityId)
        _ = assertEquals(found, Some(expect))
      } yield ()
    }

  test("find by start-time"):
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        location <- RActivityLocation.insert(Path("/home/user/fit")).transact(xa)
        activityId <- RActivity
          .insert(testActivity.copy(locationId = location.id))
          .transact(xa)
        recordId <- RActivitySession
          .insert(testActivitySession.copy(activityId = activityId))
          .transact(xa)
        found <- RActivitySession
          .findByStartTime(
            testActivitySession.startTime,
            60,
            testActivitySession.sport.some,
            None
          )
          .transact(xa)
        expect = List(testActivitySession.copy(id = recordId, activityId = activityId))
        _ = assertEquals(found, expect)
      } yield ()
    }
