package fit4s.cli.location

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.activities.LocationRepo.MoveResult
import fit4s.activities.data.LocationId
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object MoveCmd extends SharedOpts {

  case class Options(idOrPath: Either[LocationId, Path], target: Path, withFs: Boolean)

  val opts: Opts[Options] = {
    val locId = Opts
      .option[LocationId]("id", "Use a location id to move")
      .map(_.asLeft[Path])
    val path = Opts
      .option[Path]("path", "Use a path to move")
      .map(_.asRight[LocationId])
    val withFs = Opts.flag("with-fs", "Also move on the filesystem").orFalse
    val target = Opts.argument[Path]("target")

    (locId.orElse(path), target, withFs).mapN(Options.apply)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      for {
        result <- log.locationRepository.move(opts.idOrPath, opts.target, opts.withFs)
        _ <- result match {
          case MoveResult.Success => IO.println(s"Moved to ${opts.target}")
          case MoveResult.NotFound =>
            IO.println(s"Location not found, either in DB or filesystem")
          case MoveResult.FsFailure(ex) =>
            IO.println(s"There was an error: ${ex.getMessage}")
        }
      } yield ExitCode.Success
    }
}
