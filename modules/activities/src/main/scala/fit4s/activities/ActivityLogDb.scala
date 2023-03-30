package fit4s.activities

import cats.effect.kernel.Sync
import doobie.{Query => _, _}
import fit4s.activities.records.{ActivityRecord, TagRecord}

@annotation.nowarn
final class ActivityLogDb[F[_]: Sync](xa: Transactor[F]) extends ActivityLog[F] {
  override def createActivity(record: ActivityRecord) = ???

  override def insertAll = ???

  override def dropAll = ???

  override def deleteActivities(query: Query) = ???

  override def updateActivity(id: Long, record: ActivityRecord) = ???

  override def createTag(name: String) = ???

  override def deleteTag(id: Long) = ???

  override def updateTag(tag: TagRecord) = ???

  override def associateTag(tagId: Long, activityId: Long) = ???

  override def unlinkTag(tagId: Long, activityId: Long) = ???

  override def activityTags(activityId: Long) = ???

  override def activityList(query: Query) = ???

  override def activityStats(query: Query) = ???
}
