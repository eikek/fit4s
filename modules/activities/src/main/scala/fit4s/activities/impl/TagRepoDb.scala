package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.{ActivityQuery, TagRepo}
import fit4s.activities.data.{Page, TagName}
import fit4s.activities.records.{ActivityTagRecord, TagRecord}

final class TagRepoDb[F[_]: Sync](xa: Transactor[F]) extends TagRepo[F] {
  def linkTags(
      cond: Option[ActivityQuery.Condition],
      tags: NonEmptyList[TagName]
  ): F[Unit] =
    for {
      tags <- TagRecord.getOrCreate(tags.toList).transact(xa)
      tagNel = NonEmptyList.fromListUnsafe(tags)

      recreateTags = for {
        _ <- ActivityTagRecord.removeTags(tags.map(_.id))
        _ <- ActivityTagRecord.insertAll(cond, tagNel.map(_.id))
      } yield ()
      _ <- recreateTags.transact(xa)
    } yield ()

  def listTags(contains: Option[TagName], page: Page) =
    TagRecord
      .listAll(contains.map(t => s"%${t.name}%"), page)
      .transact(xa)
}

object TagRepoDb {}
