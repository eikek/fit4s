package fit4s.cli.location

import cats.effect._
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.LocationRepo.MoveResult
import fit4s.activities.data.LocationId
import fit4s.cli.{CliConfig, SharedOpts}
import fs2.io.file.Path

object DeleteCmd extends SharedOpts {

  case class Options(idOrPath: Either[LocationId, Path], withFs: Boolean)

  val opts: Opts[Options] = {
    val locId = Opts
      .option[LocationId]("id", "Use a location id to delete")
      .map(_.asLeft[Path])
    val path = Opts
      .option[Path]("path", "Use a path to delete")
      .map(_.asRight[LocationId])
    val withFs = Opts.flag("with-fs", "Also remove files in the filesystem").orFalse

    (locId.orElse(path), withFs).mapN(Options)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      for {
        result <- log.locationRepository.delete(opts.idOrPath, opts.withFs)
        _ <- result match {
          case MoveResult.Success  => IO.println(s"Deleted the location.")
          case MoveResult.NotFound => IO.println("Location not found")
          case MoveResult.FsFailure(ex) =>
            IO.println(s"Error deleting location: ${ex.getMessage}")
        }
      } yield ExitCode.Success
    }
}
