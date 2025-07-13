package fit4s.cli

import cats.effect.{ExitCode, IO}

import fit4s.cli.FormatDefinition.StringOps

import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp

object App
    extends CommandIOApp(
      name = "fit4s",
      header = "Read fit files.",
      version =
        s"${fit4s.BuildInfo.version} (#${fit4s.BuildInfo.gitHeadCommit.map(_.take(8)).getOrElse("-")})"
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

  private val serverOpts: Opts[ServerCmd.SubOpts] =
    Opts.subcommand("server", "Webview server")(ServerCmd.opts)

  private val dumpOpts: Opts[DumpCmd.SubOpts] =
    Opts.subcommand("dump", "Dump and import all the data")(DumpCmd.opts)

  val subCommandOpts: Opts[SubCommandOpts] =
    inspectOpts
      .map(SubCommandOpts.Inspect.apply)
      .orElse(activityOpts.map(SubCommandOpts.Activity.apply))
      .orElse(tagOpts.map(SubCommandOpts.Tag.apply))
      .orElse(stravaOpts.map(SubCommandOpts.Strava.apply))
      .orElse(locationOpts.map(SubCommandOpts.Location.apply))
      .orElse(initArgs.map(_ => SubCommandOpts.Init))
      .orElse(configOpts.map(SubCommandOpts.Config.apply))
      .orElse(versionOpts.map(SubCommandOpts.Version.apply))
      .orElse(serverOpts.map(SubCommandOpts.Server.apply))
      .orElse(dumpOpts.map(SubCommandOpts.Dump.apply))

  def main: Opts[IO[ExitCode]] =
    subCommandOpts.map(run).map(printError)

  def run(opts: SubCommandOpts): IO[ExitCode] =
    CliConfig.load[IO].flatMap { cliCfg =>
      opts match
        case SubCommandOpts.Inspect(c)  => InspectCmd(c)
        case SubCommandOpts.Activity(c) => ActivityCmd(cliCfg, c)
        case SubCommandOpts.Tag(c)      => TagCmd(cliCfg, c)
        case SubCommandOpts.Strava(c)   => StravaCmd(cliCfg, c)
        case SubCommandOpts.Location(c) => LocationCmd(cliCfg, c)
        case SubCommandOpts.Init        => InitCmd.init(cliCfg)
        case SubCommandOpts.Config(c)   => ConfigCmd(cliCfg, c)
        case SubCommandOpts.Version(c)  => VersionCmd(c)
        case SubCommandOpts.Server(c)   => ServerCmd(cliCfg, c)
        case SubCommandOpts.Dump(c)     => DumpCmd(cliCfg, c)
    }

  enum SubCommandOpts:
    case Inspect(opts: InspectCmd.Options)
    case Activity(opts: ActivityCmd.Options)
    case Tag(opts: TagCmd.SubOpts)
    case Strava(opts: StravaCmd.SubCmdOpts)
    case Location(opts: LocationCmd.SubOpts)
    case Init
    case Config(opts: ConfigCmd.SubOpts)
    case Version(opts: VersionCmd.Options)
    case Server(opts: ServerCmd.SubOpts)
    case Dump(opts: DumpCmd.SubOpts)

  private def printError(io: IO[ExitCode]): IO[ExitCode] =
    io.attempt.flatMap {
      case Right(code)        => IO.pure(code)
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
