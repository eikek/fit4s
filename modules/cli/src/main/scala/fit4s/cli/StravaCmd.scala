package fit4s.cli

import cats.effect.{ExitCode, IO}

import fit4s.cli.strava.*

import com.monovore.decline.Opts

object StravaCmd:

  enum SubCmdOpts:
    case Import(opts: ImportCmd.Options)
    case Authorize(opts: AuthorizeCmd.Options)
    case Logout
    case Publish(opts: LinkCmd.Options)
    case Unlink(opts: UnlinkCmd.Options)
    case Upload(opts: UploadCmd.Options)

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

  private val linkOpts: Opts[LinkCmd.Options] =
    Opts.subcommand("link", "Link activities to their strava counterpart")(LinkCmd.opts)

  private val unlinkOpts: Opts[UnlinkCmd.Options] =
    Opts.subcommand("unlink", "Remove strava ids association from activities")(
      UnlinkCmd.opts
    )

  private val uploadOpts: Opts[UploadCmd.Options] =
    Opts.subcommand("upload", "Upload activities without a strava link to strava")(
      UploadCmd.opts
    )

  val opts: Opts[SubCmdOpts] =
    importOpts
      .map(SubCmdOpts.Import.apply)
      .orElse(authorizeOpts.map(SubCmdOpts.Authorize.apply))
      .orElse(logoutOpts.map(_ => SubCmdOpts.Logout))
      .orElse(linkOpts.map(SubCmdOpts.Publish.apply))
      .orElse(unlinkOpts.map(SubCmdOpts.Unlink.apply))
      .orElse(uploadOpts.map(SubCmdOpts.Upload.apply))

  def apply(cliConfig: CliConfig, opts: SubCmdOpts): IO[ExitCode] =
    opts match
      case SubCmdOpts.Import(c)    => ImportCmd(cliConfig, c)
      case SubCmdOpts.Authorize(c) => AuthorizeCmd(cliConfig, c)
      case SubCmdOpts.Logout       => LogoutCmd(cliConfig)
      case SubCmdOpts.Publish(c)   => LinkCmd(cliConfig, c)
      case SubCmdOpts.Unlink(c)    => UnlinkCmd(cliConfig, c)
      case SubCmdOpts.Upload(c)    => UploadCmd(cliConfig, c)
