package fit4s.activities

import cats.effect.Sync
import cats.implicits._

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult

final class FlywayMigrate[F[_]: Sync](jdbc: JdbcConfig) {

  private def createLocations(folder: String) =
    List(s"classpath:db/$folder/h2", s"classpath:db/$folder/common")

  def createFlyway: F[Flyway] =
    for {
      locations <- Sync[F].pure(createLocations("migration"))
      fw = Flyway
        .configure()
        .cleanDisabled(true)
        .dataSource(jdbc.url, jdbc.user, jdbc.password)
        .locations(locations: _*)
        .load()
    } yield fw

  def run: F[MigrateResult] =
    runMain

  def runMain: F[MigrateResult] =
    for {
      fw <- createFlyway
      result <- Sync[F].blocking(fw.migrate())
    } yield result
}

object FlywayMigrate {
  def apply[F[_]: Sync](jdbcConfig: JdbcConfig): FlywayMigrate[F] =
    new FlywayMigrate[F](jdbcConfig)
}
