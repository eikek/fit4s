package fit4s.cli

import cats.effect.{ExitCode, IO}

import fit4s.buildinfo.BuildInfo

import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object App
    extends CommandIOApp(
      name = "fit4s",
      header = "Read fit files.",
      version = BuildInfo.version
    ) {

  private val inspectOpts: Opts[InspectCmd.Options] =
    Opts.subcommand("inspect", "Inspect a FIT file by converting it to json.")(
      InspectCmd.opts
    )

  private val trackOpts: Opts[TrackCmd.Options] =
    Opts.subcommand("track", "Show the track as an encoded polyline.")(TrackCmd.opts)

  private val viewOpts: Opts[ViewCmd.Options] =
    Opts.subcommand("view", "View the data in the fit in a browser.")(ViewCmd.opts)

  val subCommandOpts: Opts[SubCommandOpts] =
    inspectOpts
      .map(SubCommandOpts.Inspect.apply)
      .orElse(trackOpts.map(SubCommandOpts.Track.apply))
      .orElse(viewOpts.map(SubCommandOpts.View.apply))

  def main: Opts[IO[ExitCode]] =
    subCommandOpts.map(run).map(printError)

  def run(opts: SubCommandOpts): IO[ExitCode] =
    opts match
      case SubCommandOpts.Inspect(c) => InspectCmd(c)
      case SubCommandOpts.Track(c)   => TrackCmd(c)
      case SubCommandOpts.View(c)    => ViewCmd(c)

  enum SubCommandOpts:
    case Inspect(opts: InspectCmd.Options)
    case Track(opts: TrackCmd.Options)
    case View(opts: ViewCmd.Options)

  private def printError(io: IO[ExitCode]): IO[ExitCode] =
    io.attempt.flatMap {
      case Right(code)        => IO.pure(code)
      case Left(ex: CliError) =>
        IO.println(
          s"ERROR ${ex.getMessage}".in(Styles.error)
        ).as(ExitCode.Error)
      case Left(ex) =>
        ex.printStackTrace()
        IO.println(
          s"ERROR ${ex.getClass.getSimpleName}: ${ex.getMessage}".in(Styles.error)
        ).as(ExitCode.Error)
    }

  extension (self: String)
    def in(s: Styles): String =
      if (self.isBlank) ""
      else s"${s.style}$self${Console.RESET}"

}
