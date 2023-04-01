package fit4s.activities.impl

import cats.effect.IO
import doobie.implicits._
import fit4s.activities.{DatabaseTest, FlywayMigrate}
import org.scalacheck.Prop.forAll
import org.scalacheck.Test
import org.scalacheck.Test.Parameters

class ActivityQueryBuilderTest extends DatabaseTest {
  override def munitFixtures = List(h2DataSource)

  test("generate syntactically correct query") {
    val (jdbc, ds) = h2DataSource()
    DatabaseTest.makeXA(ds).use { xa =>
      for {
        _ <- FlywayMigrate[IO](jdbc).run
        _ <- deleteAllData(xa)
        prop = forAll(ActivityQueryGenerator.generator) { query =>
          val connIO = ActivityQueryBuilder.buildQuery(query).to[List]
          val list = connIO.transact(xa).unsafeRunSync()
          list == Nil
        }
        result = Test.check(Parameters.default, prop)
        _ <-
          if (result.passed) IO.pure(())
          else IO.raiseError(new Exception(result.status.toString))
      } yield prop
    }
  }
}
