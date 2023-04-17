package fit4s.cli

import cats.effect.{ExitCode, IO}

import fit4s.cli.activity._

import com.monovore.decline.Opts

object ActivityCmd extends SharedOpts {

  val importArgs: Opts[ImportCmd.Options] =
    Opts.subcommand(
      "import",
      "Import fit files from given directories. If given a file, it is read and each line not starting with '#' is treated as a directory to import"
    )(ImportCmd.opts)

  val summaryArgs: Opts[SummaryCmd.Options] =
    Opts.subcommand("summary", "Show an activity summary for the query.")(SummaryCmd.opts)

  val listArgs: Opts[ListCmd.Options] =
    Opts.subcommand("list", "List activities")(ListCmd.opts)

  val updateArgs: Opts[UpdateCmd.Options] =
    Opts.subcommand("update", "Import new fit files from known locations")(UpdateCmd.opts)

  val deleteArgs: Opts[DeleteCmd.Options] =
    Opts.subcommand("delete", "Delete activities by their ids")(DeleteCmd.opts)

  val showArgs: Opts[ShowCmd.Options] =
    Opts.subcommand("show", "Show details of an activity")(ShowCmd.opts)

  val setNameArgs: Opts[SetNameCmd.Options] =
    Opts.subcommand("set-name", "Set an activity name")(SetNameCmd.opts)

  val setNotesArgs: Opts[SetNotesCmd.Options] =
    Opts.subcommand("set-notes", "Set activity notes")(SetNotesCmd.opts)

  val opts = importArgs
    .map(Options.Import)
    .orElse(summaryArgs.map(Options.Summary))
    .orElse(listArgs.map(Options.List))
    .orElse(updateArgs.map(Options.Update))
    .orElse(deleteArgs.map(Options.Delete))
    .orElse(showArgs.map(Options.Show))
    .orElse(setNameArgs.map(Options.SetName))
    .orElse(setNotesArgs.map(Options.SetNotes))

  sealed trait Options extends Product
  object Options {
    final case class Import(cfg: ImportCmd.Options) extends Options
    final case class Summary(cfg: SummaryCmd.Options) extends Options
    final case class List(cfg: ListCmd.Options) extends Options
    final case class Update(cfg: UpdateCmd.Options) extends Options
    final case class Delete(cfg: DeleteCmd.Options) extends Options
    final case class Show(cfg: ShowCmd.Options) extends Options
    final case class SetName(cfg: SetNameCmd.Options) extends Options
    final case class SetNotes(cfg: SetNotesCmd.Options) extends Options
  }

  def apply(cliCfg: CliConfig, cfg: Options): IO[ExitCode] =
    cfg match {
      case Options.Import(c)   => ImportCmd(cliCfg, c)
      case Options.Summary(c)  => SummaryCmd(cliCfg, c)
      case Options.List(c)     => ListCmd(cliCfg, c)
      case Options.Update(c)   => UpdateCmd(cliCfg, c)
      case Options.Delete(c)   => DeleteCmd(cliCfg, c)
      case Options.Show(c)     => ShowCmd(cliCfg, c)
      case Options.SetName(c)  => SetNameCmd(cliCfg, c)
      case Options.SetNotes(c) => SetNotesCmd(cliCfg, c)
    }
}
