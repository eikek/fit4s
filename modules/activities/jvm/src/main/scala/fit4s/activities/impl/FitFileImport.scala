package fit4s.activities.impl

import java.time.{Instant, ZoneId}

import cats.effect.*
import cats.syntax.all.*
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}

import fit4s.activities.ImportResult
import fit4s.activities.data.{ActivityId, LocationId, TagId}
import fit4s.{ActivityReader, FitFile}

import doobie.*
import scodec.Attempt

object FitFileImport:

  def addSingle[F[_]: Files: Sync: Compression](
      tags: Set[TagId],
      notes: Option[String],
      locationId: LocationId,
      relativePath: String,
      zoneId: ZoneId,
      now: Instant
  )(fitFile: Path): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    Stream
      .eval(readFitFile(fitFile))
      .flatMap:
        case Attempt.Successful(fits) =>
          Stream.emits(
            fits.map(addFitFile(tags, notes, locationId, zoneId, now)(_, relativePath))
          )

        case Attempt.Failure(err) =>
          Stream.emit(Sync[ConnectionIO].pure(ImportResult.readFitError(err)))

  def replaceSingle[F[_]: Files: Sync: Compression](zoneId: ZoneId, now: Instant)(
      fitFile: Path
  ): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    Stream.eval(readFitFile(fitFile)).flatMap {
      case Attempt.Successful(fits) =>
        Stream.emits(fits.map(replaceFitFile(zoneId, now)))

      case Attempt.Failure(err) =>
        Stream.emit(Sync[ConnectionIO].pure(ImportResult.readFitError(err)))
    }

  def addFitFile(
      tags: Set[TagId],
      notes: Option[String],
      locationId: LocationId,
      zoneId: ZoneId,
      now: Instant
  )(
      fitFile: FitFile,
      relativePath: String
  ): ConnectionIO[ImportResult[ActivityId]] =
    ActivityReader.read(fitFile, zoneId).map(ActivityReader.fixMissingValues) match
      case Left(err)     => Sync[ConnectionIO].pure(ImportResult.activityDecodeError(err))
      case Right(result) =>
        ActivityImport.addActivity(tags, locationId, relativePath, notes, zoneId, now)(
          result
        )

  def replaceFitFile(zoneId: ZoneId, now: Instant)(
      fitFile: FitFile
  ): ConnectionIO[ImportResult[ActivityId]] =
    ActivityReader.read(fitFile, zoneId).map(ActivityReader.fixMissingValues) match
      case Left(err)     => Sync[ConnectionIO].pure(ImportResult.activityDecodeError(err))
      case Right(result) => ActivityImport.replace(now, result)

  def readFitFile[F[_]: Sync: Files: Compression](file: Path): F[Attempt[List[FitFile]]] =
    readFileOrGz[F](file).compile
      .foldChunks[Chunk[Byte]](Chunk.empty[Byte])(_ ++ _)
      .map(_.toByteVector)
      .map(FitFile.decode)
      .map(_.map(_.toList.filter(isMain)))

  private def readFileOrGz[F[_]: Files: Compression](file: Path): Stream[F, Byte] =
    if (file.extName.equalsIgnoreCase(".gz"))
      Files[F]
        .readAll(file)
        .through(fs2.compression.Compression[F].gunzip())
        .flatMap(_.content)
    else Files[F].readAll(file)

  private def isMain(fit: FitFile) =
    fit.findFileId.isRight
