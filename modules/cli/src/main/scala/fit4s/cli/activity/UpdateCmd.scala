package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import fs2.io.file.Path

import fit4s.activities.ImportCallback
import fit4s.activities.data.TagName
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object UpdateCmd extends SharedOpts {

  private val maxConcurrent =
    math.max(1, Runtime.getRuntime.availableProcessors() - 2)

  final case class Options(
      tags: List[TagName],
      sequential: Boolean
  ) {
    val parallel = !sequential
  }

  val opts: Opts[Options] =
    (initialTags, sequential).mapN(Options.apply)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      log
        .syncNewFiles(
          cliCfg.timezone,
          opts.tags.toSet,
          printFile,
          if (opts.parallel) maxConcurrent else 1
        )
        .evalTap(res => if (opts.parallel) IO.unit else IO.println(res.show))
        .map(ImportCmd.Result.apply)
        .compile
        .foldMonoid
        .flatTap(r => IO.println(s"\nDone. $r"))
        .as(ExitCode.Success)
    }

  def printFile: ImportCallback[IO] = (file: Path) => IO.print(s"\r$file ...      ")
}
