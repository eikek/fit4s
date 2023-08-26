package fit4s.cli

import cats.effect.*

import fit4s.cli.server.*
import fit4s.webview.server.Fit4sServer

import com.monovore.decline.Opts

object ServerCmd extends SharedOpts:

  enum SubOpts:
    case Start(cfg: StartCmd.Options)

  val opts: Opts[SubOpts] =
    Opts
      .subcommand("start", "Start the webview server")(StartCmd.opts)
      .map(SubOpts.Start.apply)

  def apply(cliConfig: CliConfig, opts: SubOpts): IO[ExitCode] =
    opts match
      case SubOpts.Start(c) => StartCmd(cliConfig, c)
