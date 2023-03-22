package fit4s.cli

import cats.effect.{ExitCode, IO}
import fs2.io.file.Path

object InspectCmd {

  final case class Config(
      fitFile: Path
  )

  def apply(cfg: Config): IO[ExitCode] =
    IO.pure(ExitCode.Success)
}
