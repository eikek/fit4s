package fit4s.cli

import cats.effect.{ExitCode, IO}

object ActivityCmd {

  sealed trait Config
  object Config {
    val none: Config = new Config {}
  }

  def apply(cfg: Config): IO[ExitCode] =
    IO.pure(ExitCode.Success)

}
