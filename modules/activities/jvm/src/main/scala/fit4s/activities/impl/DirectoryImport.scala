package fit4s.activities.impl

import java.time.{Instant, ZoneId}

import cats.effect._
import cats.syntax.all._
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream}

import fit4s.activities.data._
import fit4s.activities.{ImportCallback, ImportResult}

import doobie._

object DirectoryImport:

  def update[F[_]: Sync: Files: Compression](
      tags: Set[TagId],
      zoneId: ZoneId,
      now: Instant,
      latestImport: Instant,
      callback: ImportCallback[F]
  )(
      location: Location
  ): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    findActivityFiles(location.locationPath)
      .evalFilter { p =>
        val created = Files[F]
          .getBasicFileAttributes(p.path)
          .map(_.creationTime)

        created.map(_.toMillis > latestImport.toEpochMilli)
      }
      .through(resolveLocation(location.locationPath))
      .flatMap { case (file, path) =>
        Stream.eval(callback.onFile(file.path)).drain ++
          file.fold(
            fit =>
              FitFileImport.addSingle(
                tags,
                None,
                location.id,
                path,
                zoneId,
                now
              )(fit.path),
            tcx =>
              TcxFileImport.addSingle(
                tags,
                None,
                location.id,
                path,
                zoneId,
                now
              )(tcx.path)
          )
      }

  def add[F[_]: Sync: Files: Compression](
      tags: Set[TagId],
      locationId: LocationId,
      zoneId: ZoneId,
      now: Instant,
      callback: ImportCallback[F]
  )(
      dir: Path
  ): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    findActivityFiles(dir)
      .through(resolveLocation(dir))
      .flatMap { case (file, path) =>
        Stream.eval(callback.onFile(file.path)).drain ++
          file.fold(
            fit =>
              FitFileImport.addSingle(tags, None, locationId, path, zoneId, now)(
                fit.path
              ),
            tcx =>
              TcxFileImport.addSingle(tags, None, locationId, path, zoneId, now)(tcx.path)
          )
      }

  private def findActivityFiles[F[_]: Files](dir: Path): Stream[F, ActivityFile] =
    Files[F]
      .walk(dir)
      .map(ActivityFile.apply)
      .unNone
      .evalFilter(f => Files[F].isRegularFile(f.path))

  private def resolveLocation[F[_]](
      dir: Path
  ): Pipe[F, ActivityFile, (ActivityFile, String)] =
    _.map(file => file -> dir.relativize(file.path).toString)
