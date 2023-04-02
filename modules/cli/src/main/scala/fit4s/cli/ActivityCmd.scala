package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.activity.{ImportCmd, InitCmd, SummaryCmd, WeekSummaryCmd}

object ActivityCmd extends BasicOpts {

  val importArgs: Opts[ImportCmd.Config] =
    Opts.subcommand("import", "Import fit files") {
      fileOrDirArgs.map(ImportCmd.Config.apply)
    }

  val weekSummaryArgs: Opts[WeekSummaryCmd.Config] =
    Opts.subcommand("week", "Show a activity summary for the current week") {
      val sport = BasicOpts.sport.orNone

      (sport, excludeTags, includeTags).mapN(WeekSummaryCmd.Config.apply)
    }

  val summaryArgs: Opts[SummaryCmd.Config] =
    Opts.subcommand("summary", "Show an activity summary for the query.") {
      Opts
        .arguments[String]()
        .orEmpty
        .map(_.mkString(" "))
        .map(Option(_))
        .map(_.filter(_.nonEmpty))
        .map(SummaryCmd.Config)
    }

  val initArgs: Opts[Unit] =
    Opts.subcommand("init", "Initialize the database.") {
      Opts.unit
    }

  val opts = importArgs
    .map(Config.Import)
    .orElse(weekSummaryArgs.map(Config.WeekSummary))
    .orElse(initArgs.as(Config.Init))
    .orElse(summaryArgs.map(Config.Summary))

  sealed trait Config extends Product
  object Config {
    final case class Import(cfg: ImportCmd.Config) extends Config
    final case class WeekSummary(cfg: WeekSummaryCmd.Config) extends Config
    final case class Summary(cfg: SummaryCmd.Config) extends Config
    final case object Init extends Config
  }

  def apply(cfg: Config): IO[ExitCode] =
    cfg match {
      case Config.Import(c)      => ImportCmd(c)
      case Config.WeekSummary(c) => WeekSummaryCmd(c)
      case Config.Summary(c)     => SummaryCmd(c)
      case Config.Init           => InitCmd.init
    }
}
