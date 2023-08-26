package fit4s.cli.server

import cats.effect.*
import cats.syntax.all.*

import fit4s.cli.{CliConfig, SharedOpts}
import fit4s.webview.server.Fit4sServer

import com.comcast.ip4s._
import com.monovore.decline.Opts

object StartCmd extends SharedOpts:

  case class Options(bindHost: Host, bindPort: Port)

  val opts: Opts[Options] = {
    val host = Opts
      .option[Host]("host", "The host address to bind to")
      .withDefault(host"localhost")

    val port = Opts
      .option[Port]("port", "The port to bind to")
      .withDefault(port"8181")

    (host, port).mapN(Options.apply)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig)
      .flatMap(log => Fit4sServer(opts.bindHost, opts.bindPort, log, cliConfig.timezone))
      .use { server =>
        IO.println(s"Started webview server at ${server.addressIp4s}") *> IO
          .never[ExitCode]
      }
