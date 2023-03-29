package fit4s.cli.activity

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import fs2.io.file.Path

import scala.annotation.nowarn

object ImportCmd {

  final case class Config(fileOrDirectories: NonEmptyList[Path])

  @nowarn
  def apply(cfg: Config): IO[ExitCode] =
    IO.pure(ExitCode.Error)
}
