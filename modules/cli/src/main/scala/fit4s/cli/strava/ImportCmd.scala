package fit4s.cli.strava

import cats.effect.{ExitCode, IO}
import cats.kernel.Monoid
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.ActivityReader
import fit4s.activities.data.{ActivityId, TagName}
import fit4s.activities.{ImportCallback, ImportResult}
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object ImportCmd extends SharedOpts {
  private val maxConcurrent =
    math.max(1, Runtime.getRuntime.availableProcessors() - 2)

  case class Options(
      stravaExport: Path,
      tags: List[TagName],
      bikeTagPrefix: TagName,
      shoeTagPrefix: TagName,
      commuteTag: TagName,
      sequential: Boolean
  ) {
    val parallel = !sequential
  }

  val opts: Opts[Options] = {
    val tags =
      Opts.options[TagName]("tag", "Add these tags to all imported activities").orEmpty
    val file = Opts.argument[Path]("strava-export")

    (file, tags, bikeTagPrefix, shoeTagPrefix, commuteTag, sequential).mapN(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      log.strava
        .loadExport(
          opts.stravaExport,
          cliCfg.timezone,
          opts.tags.toSet,
          opts.bikeTagPrefix.some,
          opts.shoeTagPrefix.some,
          opts.commuteTag.some,
          printFile,
          if (opts.sequential) 1 else maxConcurrent
        )
        .evalTap(res => if (opts.parallel) IO.unit else IO.println(res.show))
        .map(Result.apply)
        .compile
        .foldMonoid
        .flatTap(r => IO.println(s"\nDone. $r"))
        .as(ExitCode.Success)
    }

  def printFile: ImportCallback[IO] = (file: Path) => IO.print(s"\r$file ... ")

  case class Result(success: Int, noActivity: Int, duplicates: Int, errors: Int) {
    def +(other: Result) = Result(
      success + other.success,
      noActivity + other.noActivity,
      duplicates + other.duplicates,
      errors + other.errors
    )

    override def toString =
      s"Imported: $success, Duplicates: $duplicates, not an activity: $noActivity, Errors: $errors"
  }

  object Result {
    val empty = Result(0, 0, 0, 0)
    val success = empty.copy(success = 1)
    val duplicate = empty.copy(duplicates = 1)
    val error = empty.copy(errors = 1)
    val noActivity = empty.copy(noActivity = 1)

    def apply(r: ImportResult[ActivityId]): Result =
      r match {
        case _: ImportResult.Success[_]                                    => success
        case ImportResult.Failure(_: ImportResult.FailureReason.Duplicate) => duplicate
        case ImportResult.Failure(
              ImportResult.FailureReason.ActivityDecodeError(
                ActivityReader.Failure.NoActivityFound(_, _)
              )
            ) =>
          noActivity
        case _ => error
      }

    implicit val monoid: Monoid[Result] =
      Monoid.instance(empty, _ + _)
  }
}
