package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import fit4s.cli.{CliConfig, SharedOpts}

object InitCmd extends SharedOpts {

  def init(cliCfg: CliConfig): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      log.initialize *> IO.println("Database created.").as(ExitCode.Success)
    }
}
