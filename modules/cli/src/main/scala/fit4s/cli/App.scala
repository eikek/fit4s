package fit4s.cli

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import fit4s.cli.FormatDefinition.StringOps

object App
    extends CommandIOApp(
      name = "fit4s",
      header = "Read fit files.",
      version = "0.0.1"
    )
    with SharedOpts {

  val inspectOpts: Opts[InspectCmd.Options] =
    Opts.subcommand("inspect", "Inspect a FIT file by converting it to readable json.")(
      InspectCmd.opts
    )

  val activityOpts: Opts[ActivityCmd.Options] =
    Opts.subcommand("activity", "Look into activities") {
      ActivityCmd.opts
    }

  val tagOpts: Opts[TagCmd.SubOpts] =
    Opts.subcommand("tags", "Manage tags on activities") {
      TagCmd.options
    }

  val subCommandOpts: Opts[SubCommandOpts] =
    inspectOpts
      .map(SubCommandOpts.Inspect)
      .orElse(activityOpts.map(SubCommandOpts.Activity))
      .orElse(tagOpts.map(SubCommandOpts.Tag))

  def main: Opts[IO[ExitCode]] =
    subCommandOpts.map(run).map(printError)

  def run(opts: SubCommandOpts): IO[ExitCode] =
    CliConfig.load[IO].flatMap { cliCfg =>
      opts match {
        case SubCommandOpts.Inspect(c)  => InspectCmd(c)
        case SubCommandOpts.Activity(c) => ActivityCmd(cliCfg, c)
        case SubCommandOpts.Tag(c)      => TagCmd(cliCfg, c)
      }
    }

  sealed trait SubCommandOpts
  object SubCommandOpts {
    case class Inspect(opts: InspectCmd.Options) extends SubCommandOpts
    case class Activity(opts: ActivityCmd.Options) extends SubCommandOpts
    case class Tag(opts: TagCmd.SubOpts) extends SubCommandOpts
  }

  private def printError(io: IO[ExitCode]): IO[ExitCode] =
    io.attempt.flatMap {
      case Right(code) => IO.pure(code)
      case Left(ex: CliError) =>
        IO.println(
          s"ERROR ${ex.getMessage}".in(Styles.error)
        ).as(ExitCode.Error)
      case Left(ex) =>
        IO.println(
          s"ERROR ${ex.getClass.getSimpleName}: ${ex.getMessage}".in(Styles.error)
        ).as(ExitCode.Error)
    }
}
