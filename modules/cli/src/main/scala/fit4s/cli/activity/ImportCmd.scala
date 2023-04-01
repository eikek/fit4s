package fit4s.cli.activity

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.kernel.Monoid
import fit4s.activities.data.{ActivityId, TagName}
import fit4s.activities.{ActivityLog, ImportCallback, ImportResult}
import fs2.io.file.Path

object ImportCmd {

  final case class Config(fileOrDirectories: NonEmptyList[Path])

  def apply(cfg: Config): IO[ExitCode] =
    ActivityLog.default[IO]().use { log =>
      log
        .importFromDirectories(
          Set(TagName.unsafeFromString("bike/tito")),
          printFile,
          cfg.fileOrDirectories
        )
        .evalTap {
          case ImportResult.Success(_)   => IO.println("ok.")
          case err: ImportResult.Failure => IO.println(err.messages)
        }
        .map(Result.apply)
        .compile
        .foldMonoid
        .flatTap(r => IO.println(s"\nDone. $r"))
        .as(ExitCode.Success)
    }

  def printFile: ImportCallback[IO] = (file: Path) => IO.print(s"\r$file ... ")

  case class Result(success: Int, duplicates: Int, errors: Int) {
    def +(other: Result) = Result(
      success + other.success,
      duplicates + other.duplicates,
      errors + other.errors
    )

    override def toString =
      s"Imported: $success, Duplicates: $duplicates, Errors: $errors"
  }
  object Result {
    val empty = Result(0, 0, 0)
    val success = empty.copy(success = 1)
    val duplicate = empty.copy(duplicates = 1)
    val error = empty.copy(errors = 1)

    def apply(r: ImportResult[ActivityId]): Result =
      r match {
        case _: ImportResult.Success[_]                                       => success
        case ImportResult.Failure(ImportResult.FailureReason.Duplicate(_, _)) => duplicate
        case _                                                                => error
      }

    implicit val monoid: Monoid[Result] =
      Monoid.instance(empty, _ + _)
  }
}
