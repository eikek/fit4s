package fit4s.cli.config

import cats.effect._
import fs2.Stream

import fit4s.cli._

import com.monovore.decline.Opts
import io.bullet.borer.Json

object ShowCurrentCmd extends SharedOpts {

  final case class Options(format: OutputFormat)

  val opts: Opts[Options] =
    outputFormatOpts.map(Options.apply)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    Stream
      .emit(Json.encode(cliCfg).toUtf8String)
      .evalMap(IO.println)
      .compile
      .drain
      .as(ExitCode.Success)

}
