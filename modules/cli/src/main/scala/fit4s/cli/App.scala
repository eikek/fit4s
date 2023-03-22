package fit4s.cli

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object App
    extends CommandIOApp(
      name = "fit4s",
      header = "Read fit files.",
      version = "0.0.1"
    )
    with BasicOpts {

  val inspectOpts: Opts[InspectCmd.Config] =
    Opts.subcommand("inspect", "Inspect a FIT file by converting it to readable json.") {
      fileArg.map(InspectCmd.Config.apply)
    }

  val activityOpts: Opts[ActivityCmd.Config] =
    Opts.subcommand("activity", "Look into activities") {
      Opts.apply(ActivityCmd.Config.none)
    }

  def main: Opts[IO[ExitCode]] =
    inspectOpts.orElse(activityOpts).map {
      case c: InspectCmd.Config  => InspectCmd(c)
      case c: ActivityCmd.Config => ActivityCmd(c)
    }
}
