package fit4s.cli.dump

import cats.effect.*
import cats.syntax.all.*
import fs2.compression.Compression
import fs2.io.file.{Files, Path}

import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object CreateCmd extends SharedOpts {

  final case class Options(target: Path)

  val opts: Opts[Options] = {
    val targetOpt: Opts[Path] = Opts.argument[Path](metavar = "target-file")
    targetOpt.map(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        p <- Progress[IO]
        _ <- IO.println(s"Dumping all data to file ${opts.target}")

        _ <- log.exportData
          .dump(p.progress)
          .through(Compression[IO].gzip())
          .through(Files[IO].writeAll(opts.target))
          .compile
          .drain

        summary <- p.makeSummary
        _ <- IO.println(s"\r\nExported the following data:\n$summary")
      } yield ExitCode.Success
    }
}
