package fit4s.activities

import cats.effect.IO

import org.flywaydb.core.api.output.MigrateResult

class MigrationTest extends DatabaseTest {
  override def munitFixtures =
    List(h2DataSource)

  test("flyway migration from empty database") {
    val (jdbc, _) = h2DataSource()
    val result = FlywayMigrate[IO](jdbc).run

    assertMigrationResult(result)
  }

  def assertMigrationResult(migrate: IO[MigrateResult]) =
    for {
      r1 <- migrate.map(_.migrationsExecuted)
    } yield assertEquals(r1, 1)
}
