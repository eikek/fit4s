package fit4s.cli

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import fit4s.cli.strava._

object StravaCmd {

  sealed trait SubCmdOpts
  object SubCmdOpts {
    case class Import(opts: ImportCmd.Options) extends SubCmdOpts
  }

  private val importOpts: Opts[ImportCmd.Options] =
    Opts.subcommand("import", "Import activities from a strava export file")(
      ImportCmd.opts
    )

  val opts: Opts[SubCmdOpts] =
    importOpts.map(SubCmdOpts.Import)

  def apply(cliConfig: CliConfig, opts: SubCmdOpts): IO[ExitCode] =
    opts match {
      case SubCmdOpts.Import(c) => ImportCmd(cliConfig, c)
    }
}
