package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{ActivityId, ActivityTagId, TagId}
import DoobieMeta._
import cats.data.NonEmptyList

final case class ActivityTagRecord(
    id: ActivityTagId,
    activityId: ActivityId,
    tagId: TagId
)

object ActivityTagRecord {
  private[activities] val table = fr"activity_tag"

  private[activities] def columnList(alias: Option[String]): List[Fragment] = {
    def c(name: String): Fragment =
      Fragment.const(alias.map(a => s"$a.$name").getOrElse(name))
    List(c("id"), c("activity_id"), c("tag_id"))
  }

  private val colsNoId =
    columnList(None).tail.foldSmash1(Fragment.empty, sql",", Fragment.empty)

  def insert(activityId: ActivityId, tagId: NonEmptyList[TagId]): ConnectionIO[Int] = {
    val values = tagId.toList
      .map(id => fr"($activityId, $id)")
      .foldSmash1(Fragment.empty, sql", ", Fragment.empty)

    fr"INSERT INTO $table ($colsNoId) VALUES $values".update.run
  }

  def remove(activityId: ActivityId, tagId: TagId): ConnectionIO[Int] =
    fr"DELETE FROM $table WHERE activity_id = $activityId AND tag_id = $tagId".update.run
}
