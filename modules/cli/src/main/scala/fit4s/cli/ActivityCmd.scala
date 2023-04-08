package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.activity.{ImportCmd, InitCmd, ListCmd, SummaryCmd, UpdateCmd}

object ActivityCmd extends BasicOpts {

  val importArgs: Opts[ImportCmd.Options] =
    Opts.subcommand(
      "import",
      "Import fit files from given directories. If given a file, it is read and each line not starting with '#' is treated as a directory to import"
    ) {
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

  val updateArgs: Opts[UpdateCmd.Options] =
    Opts.subcommand("update", "Import new fit files from known locations") {
      (initialTags, parallel).mapN(UpdateCmd.Options)
    }

  val opts = importArgs
    .map(Options.Import)
    .orElse(initArgs.as(Options.Init))
    .orElse(summaryArgs.map(Options.Summary))
    .orElse(listArgs.map(Options.List))
    .orElse(updateArgs.map(Options.Update))

  sealed trait Options extends Product
  object Options {
    final case class Import(cfg: ImportCmd.Options) extends Options
    final case class Summary(cfg: SummaryCmd.Options) extends Options
    final case object Init extends Options
    final case class List(cfg: ListCmd.Options) extends Options
    final case class Update(cfg: UpdateCmd.Options) extends Options
  }

  def apply(cliCfg: CliConfig, cfg: Options): IO[ExitCode] =
    cfg match {
      case Options.Import(c)  => ImportCmd(cliCfg, c)
      case Options.Summary(c) => SummaryCmd(cliCfg, c)
      case Options.Init       => InitCmd.init(cliCfg)
      case Options.List(c)    => ListCmd(cliCfg, c)
      case Options.Update(c)  => UpdateCmd(cliCfg, c)
    }
}
