package fit4s.cli

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.cli.activity.{ImportCmd, InitCmd, SummaryCmd}

object ActivityCmd extends BasicOpts {

  val importArgs: Opts[ImportCmd.Config] =
    Opts.subcommand("import", "Import fit files") {
      fileOrDirArgs.map(ImportCmd.Config.apply)
    }

  val summaryArgs: Opts[SummaryCmd.Config] =
    Opts.subcommand("summary", "Show a activity summary from imported activities") {
      val year = Opts
        .option[Int]("year", "Summary of given year")
        .orNone
      val sport = BasicOpts.sport.orNone

      (year, sport).mapN(SummaryCmd.Config.apply)
    }

  val initArgs: Opts[Unit] =
    Opts.subcommand("init", "Initialize the database.") {
      Opts.unit
    }

  val opts = importArgs
    .map(Config.Import)
    .orElse(summaryArgs.map(Config.Summary))
    .orElse(initArgs.as(Config.Init))

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
