package fit4s.cli.activity

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.kernel.Monoid
import fit4s.ActivityReader
import fit4s.activities.data.{ActivityId, TagName}
import fit4s.activities.{ActivityLog, ImportCallback, ImportResult}
import fs2.io.file.Path

object ImportCmd {
  private val maxConcurrent =
    math.max(1, Runtime.getRuntime.availableProcessors() - 2)

  final case class Config(
      fileOrDirectories: NonEmptyList[Path],
      tags: List[TagName],
      parallel: Boolean
  )

  def apply(cfg: Config): IO[ExitCode] =
    ActivityLog.default[IO]().use { log =>
      log
        .importFromDirectories(
          cfg.tags.toSet,
          printFile,
          cfg.fileOrDirectories,
          if (cfg.parallel) maxConcurrent else 1
        )
        .evalTap {
          case ImportResult.Success(_) => if (cfg.parallel) IO.unit else IO.println("ok.")
          case err: ImportResult.Failure => IO.println(err.messages)
        }
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
        case _: ImportResult.Success[_]                                       => success
        case ImportResult.Failure(ImportResult.FailureReason.Duplicate(_, _)) => duplicate
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
