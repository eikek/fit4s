package fit4s.activities.impl

import cats.data.{EitherT, OptionT}
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fs2.Stream
import fit4s.activities.LocationRepo
import fit4s.activities.LocationRepo.MoveResult
import fit4s.activities.data.{LocationId, Page}
import fit4s.activities.records.RActivityLocation
import fs2.io.file.{Files, Path}

final class LocationRepoDb[F[_]: Sync: Files](xa: Transactor[F]) extends LocationRepo[F] {
  def listLocations(
      contains: Option[String],
      page: Page
  ): Stream[F, (RActivityLocation, Long)] = {
    val search = contains.map(s => s"%$s%")
    RActivityLocation.listSream(search, page).transact(xa)
  }

  def move(
      idOrPath: Either[LocationId, Path],
      target: Path,
      withFs: Boolean
  ): F[MoveResult] = {
    def moveFs(current: RActivityLocation): EitherT[F, MoveResult, Unit] =
      if (!withFs) noop
      else
        EitherT.right(Files[F].exists(current.location)).flatMapF {
          case true =>
            Files[F]
              .move(current.location, target)
              .attempt
              .map(_.leftMap(ex => MoveResult.FsFailure(ex).widen))

          case false =>
            MoveResult.NotFound.widen.asLeft[Unit].pure[F]
        }

    loadLocation(idOrPath)
      .flatTap(moveFs)
      .semiflatTap { current =>
        RActivityLocation.setLocation(current.id, target).transact(xa)
      }
      .as(MoveResult.Success)
      .merge
  }

  def delete(idOrPath: Either[LocationId, Path], withFs: Boolean): F[MoveResult] = {
    def deleteFs(current: RActivityLocation) =
      if (!withFs) noop
      else
        EitherT.right(Files[F].exists(current.location)).flatMapF {
          case true =>
            Files[F]
              .deleteRecursively(current.location)
              .attempt
              .map(_.leftMap(ex => MoveResult.FsFailure(ex).widen))

          case false =>
            MoveResult.NotFound.widen.asLeft[Unit].pure[F]
        }

    loadLocation(idOrPath)
      .flatTap(deleteFs)
      .semiflatTap { current =>
        RActivityLocation.delete(current.id).transact(xa)
      }
      .as(MoveResult.Success)
      .merge
  }

  private val noop: EitherT[F, MoveResult, Unit] = EitherT.right(().pure[F])

  private def loadLocation(
      idOrPath: Either[LocationId, Path]
  ): EitherT[F, MoveResult, RActivityLocation] =
    OptionT(
      idOrPath
        .fold(RActivityLocation.findById, RActivityLocation.findByPath)
        .transact(xa)
    ).toRight(MoveResult.NotFound.widen)

}
