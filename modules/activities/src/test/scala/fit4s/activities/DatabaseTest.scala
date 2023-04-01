package fit4s.activities

import cats.effect._
import cats.syntax.option._
import doobie._
import doobie.implicits._
import fit4s.activities.records._
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite
import org.h2.jdbcx.{JdbcConnectionPool, JdbcDataSource}

import java.util.UUID
import javax.sql.DataSource

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
      _ <- sql"DELETE FROM ${ActivitySessionDataRecord.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${ActivitySessionRecord.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${ActivityRecord.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${ActivityLocationRecord.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${ActivityTagRecord.table}".update.run.transact(xa)
      _ <- sql"DELETE FROM ${TagRecord.table}".update.run.transact(xa)
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
