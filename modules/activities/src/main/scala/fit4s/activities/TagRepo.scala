package fit4s.activities

import fit4s.activities.data.{Page, TagId, TagName}
import fit4s.activities.records.TagRecord
import fs2.Stream

trait TagRepo[F[_]] {
  def createTag(name: TagName): F[ImportResult[TagRecord]]

  def deleteTag(id: TagId): F[Int]

  def updateTag(tag: TagRecord): F[Int]

  def listTags(contains: Option[TagName], page: Page): Stream[F, TagRecord]
}
