package fit4s.activities.impl

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.records.{
  RActivityLocation,
  RActivitySessionData,
  RActivitySession
}
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import fit4s.{ActivityReader, FitFile, FitTestData}
import fs2.io.file.Path

import java.time.{Instant, ZoneId}

class ActivityImportTest extends DatabaseTest {
  override def munitFixtures = Seq(h2DataSource)

  val now = Instant.parse("2023-04-07T11:36:30Z")
  val zone: ZoneId = ZoneId.systemDefault()

  test("insert example activity") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        data <- FitTestData.exampleActivity

        loc <- RActivityLocation.insert(Path("/home/user/test")).transact(xa)
        fit = FitFile.decodeUnsafe(data)
        result = ActivityReader
          .read(fit.head, zone)
          .fold(err => sys.error(err.toString), identity)
        idResult <- ActivityImport
          .add(loc.id, "x.fit", "Morning Ride", None, now)(result)
          .transact(xa)
        id = idResult.toEither.fold(err => sys.error(err.messages), identity)
        sessCount <- RActivitySession.countAll.transact(xa)
        recCount <- RActivitySessionData.countAll.transact(xa)
        _ = assertEquals(sessCount, result.sessions.size.toLong)
        _ = assertEquals(recCount, result.records.values.map(_.size).sum.toLong)
        _ = assert(id.id > 0)
      } yield ()
    }
  }
}
