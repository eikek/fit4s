package fit4s.activities

import fs2.Stream
import fs2.io.file.Path

import fit4s.activities.LocationRepo.MoveResult
import fit4s.activities.data.{Location, LocationId, Page}

trait LocationRepo[F[_]]:

  def listLocations(
      contains: Option[String],
      page: Page
  ): Stream[F, (Location, Long)]

  def move(
      idOrPath: Either[LocationId, Path],
      target: Path,
      withFs: Boolean
  ): F[MoveResult]

  def delete(idOrPath: Either[LocationId, Path], withFs: Boolean): F[MoveResult]

object LocationRepo:

  sealed trait MoveResult extends Product:
    def widen: MoveResult = this
  object MoveResult:
    case object Success extends MoveResult
    case class FsFailure(ex: Throwable) extends MoveResult
    case object NotFound extends MoveResult
