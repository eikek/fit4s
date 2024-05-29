package fit4s.activities

import cats.effect.Sync
import cats.implicits._

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

final class FlywayMigrate[F[_]: Sync](jdbc: JdbcConfig):

  private def createLocations(dbms: JdbcConfig.Dbms, folder: String) =
    val dbFolder = dbms match
      case JdbcConfig.Dbms.H2       => "h2"
      case JdbcConfig.Dbms.Postgres => "postgres"
    List(s"classpath:db/$folder/$dbFolder", s"classpath:db/$folder/common")

  def createFlyway: F[Flyway] =
    for {
      dbms <- jdbc.getDbms.fold(
        err => Sync[F].raiseError(new Exception(err)),
        _.pure[F]
      )
      locations <- Sync[F].pure(createLocations(dbms, "migration"))
      fw = Flyway
        .configure()
        .cleanDisabled(true)
        .dataSource(jdbc.url, jdbc.user, jdbc.password)
        .locations(locations*)
        .load()
    } yield fw

  def run: F[MigrateResult] =
    runMain

  def runMain: F[MigrateResult] =
    for {
      fw <- createFlyway
      result <- Sync[F].blocking(fw.migrate())
    } yield result

object FlywayMigrate:
  def apply[F[_]: Sync](jdbcConfig: JdbcConfig): FlywayMigrate[F] =
    new FlywayMigrate[F](jdbcConfig)
