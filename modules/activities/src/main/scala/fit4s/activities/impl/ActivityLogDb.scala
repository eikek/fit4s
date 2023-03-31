package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import doobie.{Query => _, _}
import fit4s.activities.data.{ActivityId, TagId, TagName}
import fit4s.activities.records.{ActivityRecord, TagRecord}
import fit4s.activities._
import fs2.Pipe
import fs2.io.file.Path

@annotation.nowarn
final class ActivityLogDb[F[_]: Sync](jdbcConfig: JdbcConfig, xa: Transactor[F])
    extends ActivityLog[F] {

  override def initialize: F[Unit] =
    FlywayMigrate[F](jdbcConfig).run.flatMap { result =>
      if (result.success) Sync[F].unit
      else Sync[F].raiseError(new Exception(s"Database initialization failed! $result"))
    }

  override def createActivity(record: ActivityRecord): F[InsertResult] = ???

  override def importFromDirectories(tagged: Set[TagName]): Pipe[F, Path, InsertResult] =
    ???

  override def deleteActivities(query: Query): F[Int] = ???

  override def linkTag(tagId: TagId, activityId: ActivityId): F[InsertResult] = ???

  override def unlinkTag(tagId: TagId, activityId: ActivityId): F[Int] = ???

  override def activityTags(activityId: ActivityId): fs2.Stream[F, TagRecord] = ???

  override def activityList(query: Query): fs2.Stream[F, ActivityRecord] = ???

  override def activityStats(query: Query): F[ActivityStats] = ???

  override def tagRepository: TagRepo[F] = ???

  override def locationRepository: LocationRepo[F] = ???
}
