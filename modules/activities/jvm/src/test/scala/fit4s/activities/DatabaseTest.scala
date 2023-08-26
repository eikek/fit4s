package fit4s.activities

import java.util.UUID
import javax.sql.DataSource

import cats.effect._
import cats.syntax.option._
import fs2.io.file.{Files, Path}

import fit4s.activities.records._

import doobie._
import doobie.implicits._
import munit.CatsEffectSuite
import org.h2.jdbcx.{JdbcConnectionPool, JdbcDataSource}

trait DatabaseTest extends CatsEffectSuite {

  val cio: Sync[ConnectionIO] = Sync[ConnectionIO]

  lazy val h2DataSource = ResourceSuiteLocalFixture(
    "h2DataSource", {
      val jdbc = DatabaseTest.memoryDB(UUID.randomUUID().toString)
      DatabaseTest.dataSource(jdbc).map(ds => (jdbc, ds))
    }
  )

  lazy val h2FileDataSource = ResourceSuiteLocalFixture(
    "h2FileDataSource",
    for {
      file <- Files[IO].tempFile(Path("target").some, "h2-test-", ".db", None)
      jdbc = DatabaseTest.fileDB(file)
      res <- DatabaseTest.dataSource(jdbc).map(ds => (jdbc, ds))
    } yield res
  )

  lazy val newH2DataSource = ResourceFixture(for {
    jdbc <- Resource.eval(IO(DatabaseTest.memoryDB(UUID.randomUUID().toString)))
    ds <- DatabaseTest.dataSource(jdbc)
  } yield (jdbc, ds))

  def deleteAllData(xa: Transactor[IO]): IO[Unit] =
    for {
      _ <- sql"DELETE FROM ${RActivityGeoPlace.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RGeoPlace.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RActivitySessionData.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RActivitySession.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RActivity.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RActivityLocation.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RActivityTag.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${RTag.table}".update.run.transact(xa)
    } yield ()
}

object DatabaseTest {

  def memoryDB(dbname: String): JdbcConfig =
    JdbcConfig(
      s"jdbc:h2:mem:$dbname;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "sa",
      ""
    )

  def fileDB(file: Path): JdbcConfig =
    JdbcConfig(
      s"jdbc:h2:file://${file.absolute.toString};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      "sa",
      ""
    )

  def dataSource(jdbc: JdbcConfig): Resource[IO, JdbcConnectionPool] = {
    val jdbcConnPool = {
      val ds = new JdbcDataSource()
      ds.setURL(jdbc.url)
      ds.setUser(jdbc.user)
      ds.setPassword(jdbc.password)
      JdbcConnectionPool.create(ds)
    }

    Resource.make(IO(jdbcConnPool))(cp => IO(cp.dispose()))
  }

  def makeXA(ds: DataSource): Resource[IO, Transactor[IO]] =
    for {
      ec <- ExecutionContexts.cachedThreadPool[IO]
      xa = Transactor.fromDataSource[IO](ds, ec)
    } yield xa
}
