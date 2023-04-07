package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import doobie._
import fit4s.activities.data.{ActivityId, LocationId, TagId}
import fit4s.activities.records.ActivityLocationRecord
import fit4s.activities.{ImportCallback, ImportResult}
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream}

import java.time.{Instant, ZoneId}

object DirectoryImport {

  def update[F[_]: Sync: Files](
      tags: Set[TagId],
      zoneId: ZoneId,
      now: Instant,
      latestImport: Instant,
      callback: ImportCallback[F]
  )(
      location: ActivityLocationRecord
  ): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    findFitFiles(location.location)
      .evalFilter { p =>
        val created = Files[F]
          .getBasicFileAttributes(p)
          .map(_.creationTime)

        created.map(_.toMillis > latestImport.toEpochMilli)
      }
      .through(resolveLocation(location.location))
      .flatMap { case (file, path) =>
        Stream.eval(callback.onFile(file)).drain ++
          FitFileImport.addSingle(
            tags,
            None,
            location.id,
            path,
            zoneId,
            now
          )(file)
      }

  def add[F[_]: Sync: Files](
      tags: Set[TagId],
      locationId: LocationId,
      zoneId: ZoneId,
      now: Instant,
      callback: ImportCallback[F]
  )(
      dir: Path
  ): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    findFitFiles(dir)
      .through(resolveLocation(dir))
      .flatMap { case (file, path) =>
        Stream.eval(callback.onFile(file)).drain ++
          FitFileImport.addSingle(tags, None, locationId, path, zoneId, now)(file)
      }

  def findFitFiles[F[_]: Files](dir: Path): Stream[F, Path] =
    Files[F]
      .walk(dir)
      .filter(p => p.extName.equalsIgnoreCase(".fit"))
      .evalFilter(p => Files[F].isRegularFile(p))

  def resolveLocation[F[_]](dir: Path): Pipe[F, Path, (Path, String)] =
    _.map(file => file -> dir.relativize(file).toString)
}
