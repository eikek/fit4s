package fit4s.cli

import cats.effect.{ExitCode, IO}

object InitCmd extends SharedOpts {

  def init(cliCfg: CliConfig): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      log.initialize *> IO.println("Database created.").as(ExitCode.Success)
    }
}
