package fit4s.activities.impl

import cats.data.{EitherT, OptionT}
import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.io.file.{Files, Path}

import fit4s.activities.LocationRepo
import fit4s.activities.LocationRepo.MoveResult
import fit4s.activities.data.{Location, LocationId, Page}
import fit4s.activities.records.RActivityLocation

import doobie.*
import doobie.implicits.*

final class LocationRepoDb[F[_]: Sync: Files](xa: Transactor[F]) extends LocationRepo[F]:
  def listLocations(
      contains: Option[String],
      page: Page
  ): Stream[F, (Location, Long)] =
    val search = contains.map(s => s"%$s%")
    RActivityLocation
      .listSream(search, page)
      .transact(xa)

  def move(
      idOrPath: Either[LocationId, Path],
      target: Path,
      withFs: Boolean
  ): F[MoveResult] =
    def moveFs(current: Location): EitherT[F, MoveResult, Unit] =
      if (!withFs) noop
      else
        EitherT
          .right(Files[F].exists(current.locationPath))
          .flatMapF:
            case true =>
              Files[F]
                .move(current.locationPath, target)
                .attempt
                .map(_.leftMap(ex => MoveResult.FsFailure(ex).widen))

            case false =>
              MoveResult.NotFound.widen.asLeft[Unit].pure[F]

    loadLocation(idOrPath)
      .flatTap(moveFs)
      .semiflatTap { current =>
        RActivityLocation.setLocation(current.id, target).transact(xa)
      }
      .as(MoveResult.Success)
      .merge

  def delete(idOrPath: Either[LocationId, Path], withFs: Boolean): F[MoveResult] =
    def deleteFs(current: Location) =
      if (!withFs) noop
      else
        EitherT
          .right(Files[F].exists(current.locationPath))
          .flatMapF:
            case true =>
              Files[F]
                .deleteRecursively(current.locationPath)
                .attempt
                .map(_.leftMap(ex => MoveResult.FsFailure(ex).widen))

            case false =>
              MoveResult.NotFound.widen.asLeft[Unit].pure[F]

    loadLocation(idOrPath)
      .flatTap(deleteFs)
      .semiflatTap { current =>
        RActivityLocation.delete(current.id).transact(xa)
      }
      .as(MoveResult.Success)
      .merge

  private val noop: EitherT[F, MoveResult, Unit] = EitherT.right(().pure[F])

  private def loadLocation(
      idOrPath: Either[LocationId, Path]
  ): EitherT[F, MoveResult, Location] =
    OptionT(
      idOrPath
        .fold(RActivityLocation.findById, RActivityLocation.findByPath)
        .transact(xa)
    ).toRight(MoveResult.NotFound.widen)
