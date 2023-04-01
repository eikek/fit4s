package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import fit4s.activities.ActivityLog

object InitCmd {

  def init: IO[ExitCode] =
    ActivityLog.default[IO]().use { log =>
      log.initialize *> IO.println("Database created.").as(ExitCode.Success)
    }
}
