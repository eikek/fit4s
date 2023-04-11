package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import doobie.implicits._
import doobie.{Query => _, _}
import fit4s.activities.{data, _}
import fit4s.activities.data._
import fit4s.activities.records.{
  RActivity,
  RActivityGeoPlace,
  RActivityLocation,
  RActivitySession,
  RActivityTag,
  RTag
}
import fs2.Stream
import fs2.io.file.{Files, Path}

import java.time.ZoneId

final class ActivityLogDb[F[_]: Async: Files](
    jdbcConfig: JdbcConfig,
    zoneId: ZoneId,
    xa: Transactor[F],
    geoLookup: GeoLookup[F]
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

    _ <- results match {
      case ImportResult.Success(id) =>
        Stream.eval(attachGeoPlace(id))

      case ImportResult.Failure(ImportResult.FailureReason.Duplicate(id, _, _)) =>
        Stream.eval(attachGeoPlace(id))

      case _ =>
        Stream.unit
    }
  } yield results

  def attachGeoPlace(id: ActivityId): F[List[ActivityGeoPlaceId]] =
    RActivityGeoPlace.findByActivity(id).transact(xa).flatMap {
      case Nil =>
        for {
          pos <- RActivitySession.getStartPositions(id).transact(xa)
          pIds <- pos.traverse(t => geoLookup.lookup(t._2).map(t._1 -> _))
          res <- pIds.flatTraverse { case (sessionId, optPlace) =>
            optPlace match {
              case Some(p) =>
                RActivityGeoPlace
                  .insert(sessionId, p.id, PositionName.Start)
                  .transact(xa)
                  .map(List(_))
              case None =>
                List.empty[data.ActivityGeoPlaceId].pure[F]
            }
          }
        } yield res

      case list => list.map(_.id).pure[F]
    }

  def syncNewFiles(tagged: Set[TagName], callback: ImportCallback[F], concN: Int) =
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
      query: Option[ActivityQuery.Condition]
  ): F[Vector[ActivitySessionSummary]] =
    ActivityQueryBuilder.buildSummary(query).to[Vector].transact(xa)

  def deleteActivities(ids: NonEmptyList[ActivityId], hardDelete: Boolean): F[Int] =
    if (hardDelete) RActivity.delete(ids).transact(xa)
    else RActivityTag.insert2(RTag.softDelete.id, ids).transact(xa)

  val tagRepository: TagRepo[F] = new TagRepoDb[F](xa)

  val strava: StravaSupport[F] = new StravaImpl[F](zoneId, xa)

  override def locationRepository: LocationRepo[F] = ???

}
