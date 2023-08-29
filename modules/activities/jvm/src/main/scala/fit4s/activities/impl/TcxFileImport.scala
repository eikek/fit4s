package fit4s.activities.impl

import java.io.ByteArrayInputStream
import java.time.{Instant, ZoneId}

import cats.effect._
import cats.syntax.all._
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import fs2.{Chunk, Stream}

import fit4s.activities.ImportResult
import fit4s.activities.data.{ActivityId, LocationId, TagId}
import fit4s.tcx.TcxActivity
import fit4s.tcx.TcxReader
import fit4s.{ActivityReader, FitFile}

import doobie._
import scodec.Attempt

object TcxFileImport {

  def addSingle[F[_]: Files: Sync: Compression](
      tags: Set[TagId],
      notes: Option[String],
      locationId: LocationId,
      relativePath: String,
      zoneId: ZoneId,
      now: Instant
  )(tcxFile: Path): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    Stream
      .eval(readTcxFile(tcxFile))
      .flatMap {
        case Right(fits) =>
          Stream.emits(
            fits
              .map(addTcxActivity(tags, notes, locationId, zoneId, now)(_, relativePath))
          )

        case Left(err) =>
          Stream.emit(Sync[ConnectionIO].pure(ImportResult.tcxError(err)))
      }

  def addTcxActivity(
      tags: Set[TagId],
      notes: Option[String],
      locationId: LocationId,
      zoneId: ZoneId,
      now: Instant
  )(
      act: TcxActivity,
      relativePath: String
  ): ConnectionIO[ImportResult[ActivityId]] =
    TcxActivityImport.addActivity(tags, locationId, relativePath, notes, zoneId, now)(act)

  def readTcxFile[F[_]: Sync: Files: Compression](
      file: Path
  ): F[Either[Throwable, Seq[TcxActivity]]] =
    readFileOrGz[F](file).compile
      .foldChunks[Chunk[Byte]](Chunk.empty[Byte])(_ ++ _)
      .flatMap(ch =>
        Sync[F].delay {
          val in = new ByteArrayInputStream(ch.toArray)
          val node = scala.xml.XML.load(in)
          TcxReader.activities(node)
        }
      )
      .attempt

  private def readFileOrGz[F[_]: Files: Compression](file: Path): Stream[F, Byte] =
    if (file.extName.equalsIgnoreCase(".gz"))
      Files[F]
        .readAll(file)
        .through(fs2.compression.Compression[F].gunzip())
        .flatMap(_.content)
    else Files[F].readAll(file)

  private def isMain(fit: FitFile) =
    fit.findFileId.isRight
}
