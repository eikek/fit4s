package fit4s.activities.records

import doobie._
import doobie.implicits._
import fit4s.activities.data.{ActivityId, ActivityTagId, TagId}
import DoobieMeta._

final case class ActivityTagRecord(
    id: ActivityTagId,
    activityId: ActivityId,
    tagId: TagId
)

object ActivityTagRecord {
  private val table = fr"activity_tag"

  def insert(activityId: ActivityId, tagId: TagId): ConnectionIO[ActivityTagRecord] =
    fr"INSERT INTO $table (activity_id, tag_id) VALUES ($activityId, $tagId)".update
      .withUniqueGeneratedKeys[ActivityTagId]("id")
      .map(ActivityTagRecord(_, activityId, tagId))

  def remove(activityId: ActivityId, tagId: TagId): ConnectionIO[Int] =
    fr"DELETE FROM $table WHERE activity_id = $activityId AND tag_id = $tagId".update.run
}
