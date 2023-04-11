package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.{ActivityQuery, TagRepo}
import fit4s.activities.data.{Page, TagName}
import fit4s.activities.records.{RActivityTag, RTag}

final class TagRepoDb[F[_]: Sync](xa: Transactor[F]) extends TagRepo[F] {
  def linkTags(
      cond: Option[ActivityQuery.Condition],
      tags: NonEmptyList[TagName]
  ): F[Unit] =
    for {
      tags <- RTag.getOrCreate(tags.toList).transact(xa)
      tagNel = NonEmptyList.fromListUnsafe(tags)

      recreateTags = for {
        _ <- RActivityTag.removeTags(tags.map(_.id))
        _ <- RActivityTag.insertAll(cond, tagNel.map(_.id))
      } yield ()
      _ <- recreateTags.transact(xa)
    } yield ()

  def listTags(contains: Option[TagName], page: Page) =
    RTag
      .listAll(contains.map(t => s"%${t.name}%"), page)
      .transact(xa)

  def rename(from: TagName, to: TagName): F[Boolean] =
    RTag.rename(from, to).transact(xa).map(_ > 0)

  def remove(tag: TagName): F[Int] =
    RTag.delete(tag).transact(xa)
}