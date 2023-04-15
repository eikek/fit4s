package fit4s.cli

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import fit4s.cli.strava._

object StravaCmd {

  sealed trait SubCmdOpts
  object SubCmdOpts {
    case class Import(opts: ImportCmd.Options) extends SubCmdOpts
    case class Authorize(opts: AuthorizeCmd.Options) extends SubCmdOpts
    case object Logout extends SubCmdOpts
  }

  private val importOpts: Opts[ImportCmd.Options] =
    Opts.subcommand("import", "Import activities from a strava export file")(
      ImportCmd.opts
    )

  private val authorizeOpts: Opts[AuthorizeCmd.Options] =
    Opts.subcommand("authorize", "Authorize this app to access your strava data")(
      AuthorizeCmd.opts
    )

  private val logoutOpts: Opts[Unit] =
    Opts.subcommand("logout", "Remove all strava OAuth tokens")(Opts.unit)

  val opts: Opts[SubCmdOpts] =
    importOpts
      .map(SubCmdOpts.Import)
      .orElse(authorizeOpts.map(SubCmdOpts.Authorize))
      .orElse(logoutOpts.map(_ => SubCmdOpts.Logout))

  def apply(cliConfig: CliConfig, opts: SubCmdOpts): IO[ExitCode] =
    opts match {
      case SubCmdOpts.Import(c)    => ImportCmd(cliConfig, c)
      case SubCmdOpts.Authorize(c) => AuthorizeCmd(cliConfig, c)
      case SubCmdOpts.Logout       => LogoutCmd(cliConfig)
    }
}
