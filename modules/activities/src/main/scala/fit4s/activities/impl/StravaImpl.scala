package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.data.{ActivityId, TagId, TagName}
import fit4s.activities.impl.StravaExportExtract.ExportData
import fit4s.activities.records.{
  ActivityLocationRecord,
  ActivityRecord,
  ActivityTagRecord,
  TagRecord
}
import fit4s.activities.{ImportCallback, ImportResult, StravaSupport}
import fs2.Stream
import fs2.io.file.Path

import java.time.ZoneId

final class StravaImpl[F[_]: Async](zoneId: ZoneId, xa: Transactor[F])
    extends StravaSupport[F] {
  override def loadExport(
      stravaExport: Path,
      tagged: Set[TagName],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]] =
    Stream
      .resource(
        StravaExportExtract
          .activities[F](stravaExport)
      )
      .flatMap(importFrom(stravaExport, bikeTagPrefix, shoeTagPrefix, commuteTag, tagged))

  private def importFrom(
      exportLocation: Path,
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      moreTags: Set[TagName]
  )(exportData: Vector[ExportData]): Stream[F, ImportResult[ActivityId]] = {
    val bikeTags = bikeTagPrefix
      .map(makeGearTags(exportData.flatMap(_.bike)))
      .getOrElse(Set.empty)
    val shoeTags = shoeTagPrefix
      .map(makeGearTags(exportData.flatMap(_.shoe)))
      .getOrElse(Set.empty)

    val givenTags = moreTags ++ bikeTags ++ shoeTags ++ commuteTag.iterator.toSet

    val locationAndTag = for {
      loc <- ActivityLocationRecord
        .getOrCreateLocations(List(exportLocation))
        .map(_.apply(exportLocation))
      tagIds <- TagRecord
        .getOrCreate(givenTags.toList)
        .map(_.map(r => r.name -> r.id).toMap)
    } yield (loc, tagIds)

    for {
      (locId, allTags) <- Stream.eval(locationAndTag.transact(xa))
      now <- Stream.eval(Clock[F].realTimeInstant)
      entry <- Stream.emits(exportData)
      entryTags = selectTagIds(allTags, bikeTagPrefix, shoeTagPrefix, commuteTag)(entry)

      cioResult <- FitFileImport.addSingle(
        entryTags,
        None,
        locId,
        entry.relativePath,
        zoneId,
        now
      )(entry.fitFile)
      result <- Stream.eval(cioResult.transact(xa))
      _ <- Stream.eval(result match {
        case ImportResult.Failure(ImportResult.FailureReason.Duplicate(id, _, _)) =>
          // associate tags anyways
          NonEmptyList
            .fromList(entryTags.toList)
            .map { nel =>
              (ActivityTagRecord.remove(id, nel) *> ActivityTagRecord.insert1(id, nel))
                .transact(xa)
                .void
            }
            .getOrElse(Async[F].unit) *> updateNameNotes(id, entry)

        case ImportResult.Success(id) =>
          updateNameNotes(id, entry)

        case _ => Async[F].unit
      })
    } yield result
  }

  private def updateNameNotes(id: ActivityId, entry: ExportData): F[Unit] =
    for {
      _ <- entry.name.traverse_(n => ActivityRecord.updateName(id, n).transact(xa))
      _ <- entry.description.traverse_(d =>
        ActivityRecord.updateNotes(id, d).transact(xa)
      )
    } yield ()

  private def selectTagIds(
      all: Map[TagName, TagId],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName]
  )(entry: ExportData): Set[TagId] = {
    val names =
      bikeTagPrefix
        .map(makeGearTags(entry.bike.iterator.toVector))
        .getOrElse(Set.empty) ++
        shoeTagPrefix
          .map(makeGearTags(entry.shoe.iterator.toVector))
          .getOrElse(Set.empty) ++
        (if (entry.commute) commuteTag.iterator.toSet else Set.empty)

    names.foldLeft(Set.empty[TagId]) { (res, t) =>
      res ++ all.get(t).iterator.toSet
    }
  }

  private def makeGearTags(tags: Vector[String])(prefix: TagName) =
    tags
      .flatMap(TagName.fromString(_).toOption)
      .map(prefix / _)
      .toSet
}
