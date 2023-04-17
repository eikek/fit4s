package fit4s.cli.strava

import cats.effect.{ExitCode, IO}

import fit4s.cli.{CliConfig, SharedOpts}

object LogoutCmd extends SharedOpts {

  def apply(cliConfig: CliConfig): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      log.strava.deleteTokens
        .flatMap(n => IO.println(s"Deleted $n tokens."))
        .as(ExitCode.Success)
    }
}
