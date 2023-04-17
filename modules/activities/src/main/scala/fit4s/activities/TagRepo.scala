package fit4s.activities

import cats.data.NonEmptyList
import fs2.Stream

import fit4s.activities.data.{Page, TagName}
import fit4s.activities.records.RTag

trait TagRepo[F[_]] {
  def linkTags(
      cond: Option[ActivityQuery.Condition],
      tags: NonEmptyList[TagName]
  ): F[Int]

  def unlinkTags(
      cond: Option[ActivityQuery.Condition],
      tags: NonEmptyList[TagName]
  ): F[Int]

  def listTags(contains: Option[TagName], page: Page): Stream[F, RTag]

  def rename(from: TagName, to: TagName): F[Boolean]

  def remove(tag: TagName): F[Int]

}
