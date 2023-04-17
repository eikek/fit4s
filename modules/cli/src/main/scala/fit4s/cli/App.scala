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

  private val versionOpts: Opts[VersionCmd.Options] =
    Opts.subcommand("version", "Show version information")(VersionCmd.opts)

  private val inspectOpts: Opts[InspectCmd.Options] =
    Opts.subcommand("inspect", "Inspect a FIT file by converting it to readable json.")(
      InspectCmd.opts
    )

  private val initArgs: Opts[Unit] =
    Opts.subcommand("init", "Initialize the database.")(Opts.unit)

  private val activityOpts: Opts[ActivityCmd.Options] =
    Opts.subcommand("activity", "Look into activities")(ActivityCmd.opts)

  private val tagOpts: Opts[TagCmd.SubOpts] =
    Opts.subcommand("tags", "Manage tags on activities")(TagCmd.options)

  private val locationOpts: Opts[LocationCmd.SubOpts] =
    Opts.subcommand("location", "Manage locations")(LocationCmd.opts)

  private val stravaOpts: Opts[StravaCmd.SubCmdOpts] =
    Opts.subcommand("strava", "Interface to strava")(StravaCmd.opts)

  private val configOpts: Opts[ConfigCmd.SubOpts] =
    Opts.subcommand("config", "Look at the configuration")(ConfigCmd.opts)

  val subCommandOpts: Opts[SubCommandOpts] =
    inspectOpts
      .map(SubCommandOpts.Inspect)
      .orElse(activityOpts.map(SubCommandOpts.Activity))
      .orElse(tagOpts.map(SubCommandOpts.Tag))
      .orElse(stravaOpts.map(SubCommandOpts.Strava))
      .orElse(locationOpts.map(SubCommandOpts.Location))
      .orElse(initArgs.map(_ => SubCommandOpts.Init))
      .orElse(configOpts.map(SubCommandOpts.Config))
      .orElse(versionOpts.map(SubCommandOpts.Version))

  def main: Opts[IO[ExitCode]] =
    subCommandOpts.map(run).map(printError)

  def run(opts: SubCommandOpts): IO[ExitCode] =
    CliConfig.load[IO].flatMap { cliCfg =>
      opts match {
        case SubCommandOpts.Inspect(c)  => InspectCmd(c)
        case SubCommandOpts.Activity(c) => ActivityCmd(cliCfg, c)
        case SubCommandOpts.Tag(c)      => TagCmd(cliCfg, c)
        case SubCommandOpts.Strava(c)   => StravaCmd(cliCfg, c)
        case SubCommandOpts.Location(c) => LocationCmd(cliCfg, c)
        case SubCommandOpts.Init        => InitCmd.init(cliCfg)
        case SubCommandOpts.Config(c)   => ConfigCmd(cliCfg, c)
        case SubCommandOpts.Version(c)  => VersionCmd(c)
      }
    }

  sealed trait SubCommandOpts
  object SubCommandOpts {
    case class Inspect(opts: InspectCmd.Options) extends SubCommandOpts
    case class Activity(opts: ActivityCmd.Options) extends SubCommandOpts
    case class Tag(opts: TagCmd.SubOpts) extends SubCommandOpts
    case class Strava(opts: StravaCmd.SubCmdOpts) extends SubCommandOpts
    case class Location(opts: LocationCmd.SubOpts) extends SubCommandOpts
    case object Init extends SubCommandOpts
    case class Config(opts: ConfigCmd.SubOpts) extends SubCommandOpts
    case class Version(opts: VersionCmd.Options) extends SubCommandOpts
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
