package fit4s.activities.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all.*

import fit4s.activities.TagRepo
import fit4s.activities.data.*
import fit4s.activities.records.{RActivityTag, RTag}

import doobie.*
import doobie.implicits.*

final class TagRepoDb[F[_]: Sync](xa: Transactor[F]) extends TagRepo[F]:
  def linkTags(
      cond: Option[QueryCondition],
      tags: NonEmptyList[TagName]
  ): F[Int] =
    for {
      tags <- RTag.getOrCreate(tags.toList).transact(xa)
      tagNel = NonEmptyList.fromListUnsafe(tags)

      recreateTags = for {
        _ <- RActivityTag.removeTags(cond, tags.map(_.id))
        n <- RActivityTag.insertAll(cond, tagNel.map(_.id))
      } yield n
      n <- recreateTags.transact(xa)
    } yield n

  def unlinkTags(
      cond: Option[QueryCondition],
      tags: NonEmptyList[TagName]
  ): F[Int] =
    for {
      tags <- RTag.findAll(tags.toList).transact(xa)
      n <- RActivityTag.removeTags(cond, tags.map(_.id)).transact(xa)
    } yield n

  def listTags(contains: Option[TagName], page: Page) =
    RTag
      .listAll(contains.map(t => s"%${t.name}%"), page)
      .transact(xa)
      .map(t => Tag(t.id, t.name))

  def rename(from: TagName, to: TagName): F[Boolean] =
    RTag.rename(from, to).transact(xa).map(_ > 0)

  def remove(tag: TagName): F[Int] =
    RTag.delete(tag).transact(xa)

  def removeById(tagId: TagId) =
    RTag.delete(tagId).transact(xa)
