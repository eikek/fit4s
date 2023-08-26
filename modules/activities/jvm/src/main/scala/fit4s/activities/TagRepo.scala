package fit4s.activities

import cats.data.NonEmptyList
import fs2.Stream

import fit4s.activities.data._

trait TagRepo[F[_]] {
  def linkTags(
      cond: Option[QueryCondition],
      tags: NonEmptyList[TagName]
  ): F[Int]

  def unlinkTags(
      cond: Option[QueryCondition],
      tags: NonEmptyList[TagName]
  ): F[Int]

  def listTags(contains: Option[TagName], page: Page): Stream[F, Tag]

  def rename(from: TagName, to: TagName): F[Boolean]

  def remove(tag: TagName): F[Int]

  def removeById(tagId: TagId): F[Int]
}
