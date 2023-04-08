package fit4s.cli.strava

import cats.effect.{ExitCode, IO}
import cats.kernel.Monoid
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.ActivityReader
import fit4s.activities.{ActivityLog, ImportCallback, ImportResult}
import fit4s.activities.data.{ActivityId, TagName}
import fit4s.cli.{CliConfig, SharedOpts}
import fs2.io.file.Path

object ImportCmd extends SharedOpts {
  private val maxConcurrent =
    math.max(1, Runtime.getRuntime.availableProcessors() - 2)

  case class Options(
      stravaExport: Path,
      tags: List[TagName],
      bikeTagPrefix: TagName,
      shoeTagPrefix: TagName,
      commuteTag: TagName,
      parallel: Boolean
  )

  val opts: Opts[Options] = {
    val tags =
      Opts.options[TagName]("tag", "Add these tags to all imported activities").orEmpty
    val file = Opts.argument[Path]("strava-export")
    val btp = Opts
      .option[TagName]("bike-tag", "Prefix for tagging the used bike")
      .withDefault(TagName.unsafeFromString("Bike"))
    val stp = Opts
      .option[TagName]("shoe-tag", "Prefix for tagging used shoes")
      .withDefault(TagName.unsafeFromString("Shoe"))
    val commTag = Opts
      .option[TagName]("commute-tag", "Tag used to mark commutes.")
      .withDefault(TagName.unsafeFromString("Commute"))

    (file, tags, btp, stp, commTag, parallel).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    ActivityLog[IO](cliCfg.jdbcConfig, cliCfg.timezone).use { log =>
      log.strava
        .loadExport(
          opts.stravaExport,
          opts.tags.toSet,
          opts.bikeTagPrefix.some,
          opts.shoeTagPrefix.some,
          opts.commuteTag.some,
          printFile,
          if (opts.parallel) maxConcurrent else 1
        )
        .evalTap(res => IO.println(res.show))
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
