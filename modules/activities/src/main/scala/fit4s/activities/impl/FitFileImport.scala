package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import doobie._
import fit4s.activities.ImportResult
import fit4s.activities.data.{ActivityId, LocationId, TagId}
import fit4s.{ActivityReader, FitFile}
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}
import scodec.Attempt

import java.time.ZoneId

object FitFileImport {

  def addSingle[F[_]: Files: Sync](
      tags: Set[TagId],
      notes: Option[String],
      locationId: LocationId,
      relativePath: String,
      zoneId: ZoneId
  )(fitFile: Path): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    Stream
      .eval(readFitFile(fitFile))
      .flatMap {
        case Attempt.Successful(fits) =>
          Stream.emits(
            fits.map(addFitFile(tags, notes, locationId, zoneId)(_, relativePath))
          )

        case Attempt.Failure(err) =>
          Stream.emit(Sync[ConnectionIO].pure(ImportResult.readFitError(err)))
      }

  def addFitFile(
      tags: Set[TagId],
      notes: Option[String],
      locationId: LocationId,
      zoneId: ZoneId
  )(
      fitFile: FitFile,
      relativePath: String
  ): ConnectionIO[ImportResult[ActivityId]] =
    ActivityReader.read(fitFile) match {
      case Left(err) => Sync[ConnectionIO].pure(ImportResult.activityDecodeError(err))
      case Right(result) =>
        ActivityImport.addActivity(tags, locationId, relativePath, notes, zoneId)(
          result
        )
    }

  def readFitFile[F[_]: Sync: Files](file: Path): F[Attempt[List[FitFile]]] =
    Files[F]
      .readAll(file)
      .compile
      .foldChunks[Chunk[Byte]](Chunk.empty[Byte])(_ ++ _)
      .map(_.toByteVector)
      .map(FitFile.decode)
      .map(_.map(_.toList.filter(isMain)))

  private def isMain(fit: FitFile) =
    fit.findFileId.isRight
}
