package fit4s.activities.records

import doobie._
import doobie.implicits._

final case class ActivityTagRecord(id: Long, activityId: Long, tagId: Long)

object ActivityTagRecord {
  private val table = fr"activity_tag"

  def insert(activityId: Long, tagId: Long): ConnectionIO[ActivityTagRecord] =
    fr"INSERT INTO $table (activity_id, tag_id) VALUES ($activityId, $tagId)".update
      .withUniqueGeneratedKeys[Long]("id")
      .map(ActivityTagRecord(_, activityId, tagId))

  def remove(activityId: Long, tagId: Long): ConnectionIO[Int] =
    fr"DELETE FROM $table WHERE activity_id = $activityId AND tag_id = $tagId".update.run
}
