package fit4s.activities

import cats.data.NonEmptyList
import fit4s.activities.data.{Page, TagName}
import fit4s.activities.records.RTag
import fs2.Stream

trait TagRepo[F[_]] {
  def linkTags(
      cond: Option[ActivityQuery.Condition],
      tags: NonEmptyList[TagName]
  ): F[Unit]

  def listTags(contains: Option[TagName], page: Page): Stream[F, RTag]

  def rename(from: TagName, to: TagName): F[Boolean]

  def remove(tag: TagName): F[Int]
}
