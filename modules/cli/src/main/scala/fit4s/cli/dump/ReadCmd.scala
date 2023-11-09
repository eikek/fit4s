package fit4s.cli.dump

import cats.effect.*
import cats.syntax.all.*
import fs2.Pipe
import fs2.compression.Compression
import fs2.io.file.{Files, Path}

import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object ReadCmd extends SharedOpts {

  final case class Options(dryRun: Boolean, source: Path)

  val opts: Opts[Options] = {
    val targetOpt: Opts[Path] = Opts.argument[Path](metavar = "source-file")
    val dryRun: Opts[Boolean] =
      Opts.flag("dry-run", "Do not update the database, only read the file").orFalse
    (dryRun, targetOpt).mapN(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        p <- Progress[IO]
        _ <- IO.println(s"Reading in all data from dump ${opts.source}")
        _ <-
          if (opts.dryRun)
            IO.println("This is a dry run, no database operations take place")
          else IO.unit
        _ <- Files[IO]
          .readAll(opts.source)
          .through(gunzip(opts.source))
          .through(log.exportData.read(opts.dryRun, p.progress))
          .compile
          .drain

        summary <- p.makeSummary
        _ <- IO.println(s"\r\nImported the following data:\n$summary")
      } yield ExitCode.Success

    }

  private def gunzip(file: Path): Pipe[IO, Byte, Byte] =
    if (file.extName == ".gz") Compression[IO].gunzip().andThen(_.flatMap(_.content))
    else identity
}
