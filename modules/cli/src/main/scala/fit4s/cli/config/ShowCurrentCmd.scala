package fit4s.cli.config

import cats.effect.*

import fit4s.cli.*

import com.monovore.decline.Opts
import io.bullet.borer.Json

object ShowCurrentCmd extends SharedOpts {

  final case class Options(format: OutputFormat)

  val opts: Opts[Options] =
    outputFormatOpts.map(Options.apply)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    val out =
      opts.format.fold(
        Json.encode(cliCfg).toUtf8String,
        cliCfg.toString
      )
    IO.println(out).as(ExitCode.Success)
}
