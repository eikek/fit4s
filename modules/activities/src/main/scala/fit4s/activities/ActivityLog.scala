package fit4s.activities

import fit4s.activities.records.ActivityRecord
import fs2._

trait ActivityLog[F[_]] {

  def insertActivity(record: ActivityRecord): F[InsertResult]

  def insertAll: Pipe[F, ActivityRecord, InsertResult]

  def dropAll: F[Unit]

  def deleteActivities(query: Query): F[Int]

  def updateActivity(id: Long, record: ActivityRecord): F[Int]

  def activityList(query: Query): Stream[F, ActivityRecord]

  def activityStats(query: Query): F[ActivityStats]
}

object ActivityLog {
  def apply[F[_]]: F[ActivityLog[F]] =
    ???
}
