package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.activity.{ImportCmd, InitCmd, SummaryCmd}

object ActivityCmd extends BasicOpts {

  val importArgs: Opts[ImportCmd.Config] =
    Opts.subcommand("import", "Import fit files") {
      (fileOrDirArgs, initialTags, parallel).mapN(ImportCmd.Config)
    }

  val summaryArgs: Opts[SummaryCmd.Config] =
    Opts.subcommand("summary", "Show an activity summary for the query.") {
      summaryQueryOpts.map(SummaryCmd.Config)
    }

  val initArgs: Opts[Unit] =
    Opts.subcommand("init", "Initialize the database.") {
      Opts.unit
    }

  val opts = importArgs
    .map(Config.Import)
    .orElse(initArgs.as(Config.Init))
    .orElse(summaryArgs.map(Config.Summary))

  sealed trait Config extends Product
  object Config {
    final case class Import(cfg: ImportCmd.Config) extends Config
    final case class Summary(cfg: SummaryCmd.Config) extends Config
    final case object Init extends Config
  }

  def apply(cfg: Config): IO[ExitCode] =
    cfg match {
      case Config.Import(c)  => ImportCmd(c)
      case Config.Summary(c) => SummaryCmd(c)
      case Config.Init       => InitCmd.init
    }
}
