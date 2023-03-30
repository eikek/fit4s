package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import fit4s.profile.types.Sport

import scala.annotation.nowarn

object SummaryCmd {

  final case class Config(year: Option[Int], sport: Option[Sport])

  @nowarn
  def apply(cfg: Config): IO[ExitCode] =
    IO.pure(ExitCode.Error)
}
