package fit4s.cli.activity

import cats.Monoid
import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.ActivityReader
import fit4s.activities.data.{ActivityId, TagName}
import fit4s.activities.{ImportCallback, ImportResult}
import fit4s.cli.{CliConfig, CliError, SharedOpts}
import fs2.io.file.{Files, Path}

object ImportCmd extends SharedOpts {
  private val maxConcurrent =
    math.max(1, Runtime.getRuntime.availableProcessors() - 2)

  private val files: Files[IO] = Files[IO]

  final case class Options(
      fileOrDirectories: NonEmptyList[Path],
      tags: List[TagName],
      sequential: Boolean
  ) {
    val parallel = !sequential
  }

  val opts: Opts[Options] = {
    val fileOrDirArgs: Opts[NonEmptyList[Path]] =
      Opts.arguments[Path](metavar = "fileOrDirs")

    (fileOrDirArgs, initialTags, sequential).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    getDirectories(opts).flatMap { dirs =>
      activityLog(cliCfg).use { log =>
        log
          .importFromDirectories(
            opts.tags.toSet,
            printFile,
            dirs,
            if (opts.parallel) maxConcurrent else 1
          )
          .evalTap(res => if (opts.parallel) IO.unit else IO.println(res.show))
          .map(Result.apply)
          .compile
          .foldMonoid
          .flatTap(r => IO.println(s"\nDone. $r"))
          .as(ExitCode.Success)
      }
    }

  def getDirectories(options: Options): IO[NonEmptyList[Path]] =
    files.isRegularFile(options.fileOrDirectories.head).flatMap {
      case true =>
        files
          .readAll(options.fileOrDirectories.head)
          .through(fs2.text.utf8.decode)
          .through(fs2.text.lines)
          .map(_.trim)
          .filter(s => !s.startsWith("#") && s.nonEmpty)
          .map(Path.apply)
          .evalFilter(files.isDirectory)
          .compile
          .toList
          .map(NonEmptyList.fromList)
          .flatMap {
            case Some(nel) => IO.pure(nel)
            case None =>
              IO.raiseError(
                new CliError(
                  s"Text file doesn't contain lines of directories: ${options.fileOrDirectories.head}"
                )
              )
          }
      case false => IO.pure(options.fileOrDirectories)
    }

  def printFile: ImportCallback[IO] = (file: Path) => IO.print(s"\r$file ...          ")

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
