package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import fit4s.activities.ActivityLog
import fit4s.cli.CliConfig

object InitCmd {

  def init(cliCfg: CliConfig): IO[ExitCode] =
    ActivityLog[IO](cliCfg.jdbcConfig, cliCfg.timezone).use { log =>
      log.initialize *> IO.println("Database created.").as(ExitCode.Success)
    }
}
