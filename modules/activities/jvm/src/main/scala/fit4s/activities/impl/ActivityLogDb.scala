package fit4s.activities.impl

import java.time.ZoneId

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.compression.Compression
import fs2.io.file.{Files, Path}

import fit4s.activities.*
import fit4s.activities.data.*
import fit4s.activities.dump.ExportData
import fit4s.activities.records.*
import fit4s.activities.records.DoobieImplicits.given

import doobie.syntax.all.*
import doobie.{Query as _, *}

final class ActivityLogDb[F[_]: Async: Files: Compression](
    jdbcConfig: JdbcConfig,
    xa: Transactor[F],
    geoLookup: GeoLookup[F],
    stravaSupport: StravaSupport[F]
) extends ActivityLog[F]:
  private val placeAttach = new GeoPlaceAttach[F](xa, geoLookup)
  private val dumpData = ExportData(xa, jdbcConfig.dbms)

  override def initialize: F[Unit] =
    FlywayMigrate[F](jdbcConfig).run.flatMap { result =>
      if (result.success) Sync[F].unit
      else Sync[F].raiseError(new Exception(s"Database initialization failed! $result"))
    }

  override def geoLookup(ids: List[ActivityId], onId: ActivityId => F[Unit]): F[Unit] =
    RActivityGeoPlace
      .findMissingActivities(ids)
      .transact(xa)
      .evalTap(onId)
      .evalMap(placeAttach.attachGeoPlace)
      .compile
      .drain

  override def importFromDirectories(
      zoneId: ZoneId,
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

  def rereadActivities(
      zoneId: ZoneId,
      condition: QueryCondition
  ): Stream[F, (ActivityRereadData, ImportResult[ActivityId])] =
    for
      now <- Stream.eval(Clock[F].realTimeInstant)
      activity <- ActivityQueryBuilder
        .activityRereadDataFragment(condition)
        .query[ActivityRereadData]
        .streamWithChunkSize(100)
        .transact(xa)

      actFile = ActivityFile(activity.file)
      exists <- Stream.eval(Files[F].exists(activity.file))
      r <-
        if (!exists)
          Stream.emit(ImportResult.fileNotExists(activity.fileId, activity.file))
        else
          actFile match
            case Some(ActivityFile.Fit(path)) =>
              FitFileImport
                .replaceSingle(zoneId, now)(path)
                .evalMap(_.transact(xa))

            case Some(ActivityFile.Tcx(path)) =>
              TcxFileImport.replaceSingle(now)(path).evalMap(_.transact(xa))

            case None =>
              Stream.emit(ImportResult.unsupportedFile(activity.file))
    yield (activity, r)

  def syncNewFiles(
      zoneId: ZoneId,
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
        group.toNel match
          case Some(nel) =>
            ActivityQueryBuilder
              .tagsForActivity(nel.head._1.id)
              .transact(xa)
              .map(tags =>
                ActivityListResult(
                  nel.head._1,
                  nel.head._2,
                  nel.head._3,
                  nel.map(_._4),
                  tags
                )
              )

          case None =>
            Async[F].raiseError[ActivityListResult](
              new Exception("empty group by in stream")
            )
      }

  override def activitySummary(
      query: ActivityQuery
  ): F[Vector[ActivitySessionSummary]] =
    ActivityQueryBuilder.buildSummary(query).to[Vector].transact(xa)

  def activityDetails(
      id: ActivityId,
      withSessionData: Boolean
  ): F[Option[ActivityDetailResult]] =
    ActivityDetailQuery.create(id, withSessionData).transact(xa)

  def deleteActivities(ids: NonEmptyList[ActivityId], hardDelete: Boolean): F[Int] =
    if (hardDelete) RActivity.delete(ids).transact(xa)
    else RActivityTag.insert2(RTag.softDelete.id, ids).transact(xa)

  def setGeneratedActivityName(id: ActivityId, zoneId: ZoneId): F[Unit] =
    RActivity.setGeneratedName(id, zoneId).transact(xa).void

  def setActivityName(id: ActivityId, name: String): F[Unit] =
    RActivity.updateName(id, name).transact(xa).void

  def setActivityNotes(id: ActivityId, notes: Option[String]): F[Unit] =
    RActivity.updateNotes(id, notes).transact(xa).void

  val tagRepository: TagRepo[F] = new TagRepoDb[F](xa)

  val strava: StravaSupport[F] = stravaSupport

  val locationRepository: LocationRepo[F] = new LocationRepoDb[F](xa)

  val exportData: ExportData[F] = dumpData
