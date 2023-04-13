package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import doobie.implicits._
import doobie.{Query => _, _}
import fit4s.activities._
import fit4s.activities.data._
import fit4s.activities.records.{RActivity, RActivityLocation, RActivityTag, RTag}
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.time.ZoneId

final class ActivityLogDb[F[_]: Async: Files](
    jdbcConfig: JdbcConfig,
    zoneId: ZoneId,
    xa: Transactor[F],
    geoLookup: GeoLookup[F]
) extends ActivityLog[F] {
  private[this] val placeAttach = new GeoPlaceAttach[F](xa, geoLookup)

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
    tags <- Stream.eval(RTag.getOrCreate(tagged.toList).transact(xa))

    locs <- Stream.eval(
      RActivityLocation.getOrCreateLocations(dirs.toList).transact(xa)
    )

    (dir, locId) <- Stream.emits(locs.toList)

    now <- Stream.eval(Clock[F].realTimeInstant)
    doImportTask = DirectoryImport
      .add[F](tags.map(_.id).toSet, locId, zoneId, now, callback)(dir)
    results <-
      if (concN > 1) doImportTask.parEvalMap(concN)(_.transact(xa))
      else doImportTask.evalMap(_.transact(xa))

    _ <- Stream.eval(placeAttach.applyResult(results))
  } yield results

  def syncNewFiles(
      tagged: Set[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]] =
    for {
      sync <- Stream.eval(SyncData.get.transact(xa))
      now <- Stream.eval(Clock[F].realTimeInstant)
      tags <- Stream.eval(RTag.getOrCreate(tagged.toList).transact(xa))

      updateTask = Stream
        .emits(sync.locations)
        .flatMap(
          DirectoryImport.update[F](
            tags.map(_.id).toSet,
            zoneId,
            now,
            sync.lastImport,
            callback
          )
        )
      results <-
        if (concN > 1) updateTask.parEvalMap(concN)(_.transact(xa))
        else updateTask.evalMap(_.transact(xa))

      _ <- Stream.eval(placeAttach.applyResult(results))
    } yield results

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
      query: ActivityQuery
  ): F[Vector[ActivitySessionSummary]] =
    ActivityQueryBuilder.buildSummary(query).to[Vector].transact(xa)

  def activityDetails(id: ActivityId): F[Option[ActivityDetailResult]] =
    ActivityDetailQuery.create(id).transact(xa)

  def deleteActivities(ids: NonEmptyList[ActivityId], hardDelete: Boolean): F[Int] =
    if (hardDelete) RActivity.delete(ids).transact(xa)
    else RActivityTag.insert2(RTag.softDelete.id, ids).transact(xa)

  def setActivityName(id: ActivityId, name: Option[String]): F[Unit] =
    name
      .map(RActivity.updateName(id, _))
      .getOrElse(RActivity.setGeneratedName(id, zoneId))
      .transact(xa)
      .void

  def setActivityNotes(id: ActivityId, notes: Option[String]): F[Unit] =
    RActivity.updateNotes(id, notes).transact(xa).void

  val tagRepository: TagRepo[F] = new TagRepoDb[F](xa)

  val strava: StravaSupport[F] = new StravaImpl[F](zoneId, xa, placeAttach)

  override def locationRepository: LocationRepo[F] = ???

}
