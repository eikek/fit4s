package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.activity._

object ActivityCmd extends SharedOpts {

  val importArgs: Opts[ImportCmd.Options] =
    Opts.subcommand(
      "import",
      "Import fit files from given directories. If given a file, it is read and each line not starting with '#' is treated as a directory to import"
    )(ImportCmd.opts)

  val summaryArgs: Opts[SummaryCmd.Options] =
    Opts.subcommand("summary", "Show an activity summary for the query.")(SummaryCmd.opts)

  val initArgs: Opts[Unit] =
    Opts.subcommand("init", "Initialize the database.")(Opts.unit)

  val listArgs: Opts[ListCmd.Options] =
    Opts.subcommand("list", "List activities")(ListCmd.opts)

  val updateArgs: Opts[UpdateCmd.Options] =
    Opts.subcommand("update", "Import new fit files from known locations")(UpdateCmd.opts)

  val deleteArgs: Opts[DeleteCmd.Options] =
    Opts.subcommand("delete", "Delete activities by their ids")(DeleteCmd.opts)

  val showArgs: Opts[ShowCmd.Options] =
    Opts.subcommand("show", "Show details of an activity")(ShowCmd.opts)

  val opts = importArgs
    .map(Options.Import)
    .orElse(initArgs.as(Options.Init))
    .orElse(summaryArgs.map(Options.Summary))
    .orElse(listArgs.map(Options.List))
    .orElse(updateArgs.map(Options.Update))
    .orElse(deleteArgs.map(Options.Delete))
    .orElse(showArgs.map(Options.Show))

  sealed trait Options extends Product
  object Options {
    final case class Import(cfg: ImportCmd.Options) extends Options
    final case class Summary(cfg: SummaryCmd.Options) extends Options
    final case object Init extends Options
    final case class List(cfg: ListCmd.Options) extends Options
    final case class Update(cfg: UpdateCmd.Options) extends Options
    final case class Delete(cfg: DeleteCmd.Options) extends Options
    final case class Show(cfg: ShowCmd.Options) extends Options
  }

  def apply(cliCfg: CliConfig, cfg: Options): IO[ExitCode] =
    cfg match {
      case Options.Import(c)  => ImportCmd(cliCfg, c)
      case Options.Summary(c) => SummaryCmd(cliCfg, c)
      case Options.Init       => InitCmd.init(cliCfg)
      case Options.List(c)    => ListCmd(cliCfg, c)
      case Options.Update(c)  => UpdateCmd(cliCfg, c)
      case Options.Delete(c)  => DeleteCmd(cliCfg, c)
      case Options.Show(c)    => ShowCmd(cliCfg, c)
    }
}
