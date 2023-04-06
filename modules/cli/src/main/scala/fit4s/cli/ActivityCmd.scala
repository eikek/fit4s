package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.activity.{ImportCmd, InitCmd, ListCmd, SummaryCmd}

object ActivityCmd extends BasicOpts {

  val importArgs: Opts[ImportCmd.Options] =
    Opts.subcommand("import", "Import fit files") {
      (fileOrDirArgs, initialTags, parallel).mapN(ImportCmd.Options)
    }

  val summaryArgs: Opts[SummaryCmd.Options] =
    Opts.subcommand("summary", "Show an activity summary for the query.") {
      activitySelectionOps.map(SummaryCmd.Options)
    }

  val initArgs: Opts[Unit] =
    Opts.subcommand("init", "Initialize the database.") {
      Opts.unit
    }

  val listArgs: Opts[ListCmd.Options] =
    Opts.subcommand("list", "List activities") {
      val filesOnly = Opts.flag("files-only", "Only print filenames").orFalse
      (activitySelectionOps, pageOpts, filesOnly).mapN(ListCmd.Options)
    }

  val opts = importArgs
    .map(Config.Import)
    .orElse(initArgs.as(Config.Init))
    .orElse(summaryArgs.map(Config.Summary))
    .orElse(listArgs.map(Config.List))

  sealed trait Config extends Product
  object Config {
    final case class Import(cfg: ImportCmd.Options) extends Config
    final case class Summary(cfg: SummaryCmd.Options) extends Config
    final case object Init extends Config
    final case class List(cfg: ListCmd.Options) extends Config
  }

  def apply(cfg: Config): IO[ExitCode] =
    CliConfig.load[IO].flatMap { cliCfg =>
      cfg match {
        case Config.Import(c)  => ImportCmd(cliCfg, c)
        case Config.Summary(c) => SummaryCmd(cliCfg, c)
        case Config.Init       => InitCmd.init(cliCfg)
        case Config.List(c)    => ListCmd(cliCfg, c)
      }
    }
}
