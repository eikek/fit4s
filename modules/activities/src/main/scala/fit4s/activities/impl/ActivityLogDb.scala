package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import doobie.implicits._
import doobie.{Query => _, _}
import fit4s.activities._
import fit4s.activities.data._
import fit4s.activities.records.{ActivityLocationRecord, TagRecord}
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.time.ZoneId

final class ActivityLogDb[F[_]: Async: Files](
    jdbcConfig: JdbcConfig,
    zoneId: ZoneId,
    xa: Transactor[F]
) extends ActivityLog[F] {

  override def initialize: F[Unit] =
    FlywayMigrate[F](jdbcConfig).run.flatMap { result =>
      if (result.success) Sync[F].unit
      else Sync[F].raiseError(new Exception(s"Database initialization failed! $result"))
    }

  override def importFromDirectories(
      tagged: Set[TagName],
      callback: ImportCallback[F],
      dirs: NonEmptyList[Path],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]] = for {
    isDir <- Stream.eval(dirs.traverse(p => Files[F].isDirectory(p).map(p -> _)))
    _ <-
      if (isDir.forall(_._2)) Stream.unit
      else
        Stream.raiseError[F](
          new Exception(
            s"One or more directories do not exists or aren't directories: ${isDir.filterNot(_._2).map(_._1)}"
          )
        )
    tags <- Stream.eval(TagRecord.getOrCreate(tagged.toList).transact(xa))

    locs <- Stream.eval(
      ActivityLocationRecord.getOrCreateLocations(dirs.toList).transact(xa)
    )

    (dir, locId) <- Stream.emits(locs.toList)

    doImportTask = DirectoryImport
      .add[F](tags.map(_.id).toSet, locId, zoneId, callback)(dir)
    results <-
      if (concN > 1) doImportTask.parEvalMap(concN)(_.transact(xa))
      else doImportTask.evalMap(_.transact(xa))
  } yield results

  override def deleteActivities(query: ActivityQuery): F[Int] = ???

  override def linkTag(tagId: TagId, activityId: ActivityId): F[Unit] = ???

  override def unlinkTag(tagId: TagId, activityId: ActivityId): F[Int] = ???

  override def activityTags(activityId: ActivityId): Stream[F, TagRecord] = ???

  override def activityList(
      query: ActivityQuery
  ): Stream[F, ActivityListResult] =
    ActivityQueryBuilder
      .buildQuery(query)
      .stream
      .transact(xa)
      .groupAdjacentBy(_._1.id)
      .evalMap { case (_, group) =>
        group.toNel match {
          case Some(nel) =>
            ActivityQueryBuilder
              .tagsForActivity(nel.head._1.id)
              .transact(xa)
              .map(tags =>
                ActivityListResult(nel.head._1, nel.head._2, nel.map(_._3), tags)
              )

          case None =>
            Async[F].raiseError[ActivityListResult](
              new Exception("empty group by in stream")
            )
        }
      }

  override def activitySummary(
      query: Option[ActivityQuery.Condition]
  ): F[Vector[ActivitySessionSummary]] =
    ActivityQueryBuilder.buildSummary(query).to[Vector].transact(xa)

  override def tagRepository: TagRepo[F] = ???

  override def locationRepository: LocationRepo[F] = ???
}
