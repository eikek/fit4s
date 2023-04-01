package fit4s.activities.impl

import cats.effect._
import doobie._
import fit4s.activities.data.{ActivityId, LocationId, TagId}
import fit4s.activities.{ImportCallback, ImportResult}
import fs2.io.file.{Files, Path}
import fs2.{Pipe, Stream}

import java.time.ZoneId

object DirectoryImport {

  def add[F[_]: Sync: Files](
      tags: Set[TagId],
      locationId: LocationId,
      zoneId: ZoneId,
      callback: ImportCallback[F]
  )(
      dir: Path
  ): Stream[F, ConnectionIO[ImportResult[ActivityId]]] =
    findFitFiles(dir)
      .through(resolveLocation(dir))
      .flatMap { case (file, path) =>
        Stream.eval(callback.onFile(file)).drain ++
          FitFileImport.addSingle(tags, None, locationId, path, zoneId)(file)
      }

  def findFitFiles[F[_]: Files](dir: Path): Stream[F, Path] =
    Files[F]
      .walk(dir)
      .filter(p => p.extName.equalsIgnoreCase(".fit"))
      .evalFilter(p => Files[F].isRegularFile(p))

  def resolveLocation[F[_]](dir: Path): Pipe[F, Path, (Path, String)] =
    _.map(file => file -> dir.relativize(file).toString)
}
