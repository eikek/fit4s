package fit4s.activities

import cats.effect._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import fit4s.activities.records.{ActivityRecord, TagRecord}
import fs2._
import org.h2.jdbcx.JdbcConnectionPool

trait ActivityLog[F[_]] {

  def createActivity(record: ActivityRecord): F[InsertResult]

  def insertAll: Pipe[F, ActivityRecord, InsertResult]

  def dropAll: F[Unit]

  def deleteActivities(query: Query): F[Int]

  def updateActivity(id: Long, record: ActivityRecord): F[Int]

  def createTag(name: String): F[InsertResult]

  def deleteTag(id: Long): F[Int]

  def updateTag(tag: TagRecord): F[InsertResult]

  def associateTag(tagId: Long, activityId: Long): F[InsertResult]

  def unlinkTag(tagId: Long, activityId: Long): F[Int]

  def activityTags(activityId: Long): Stream[F, TagRecord]

  def activityList(query: Query): Stream[F, ActivityRecord]

  def activityStats(query: Query): F[ActivityStats]
}

object ActivityLog {
  def apply[F[_]: Async](jdbcConfig: JdbcConfig): Resource[F, ActivityLog[F]] = {
    val pool = JdbcConnectionPool.create(
      jdbcConfig.url,
      jdbcConfig.user,
      jdbcConfig.password
    )

    for {
      _ <- Resource.eval(FlywayMigrate[F](jdbcConfig).run)
      ec <- ExecutionContexts.fixedThreadPool(10)
      ds <- Resource.make(Sync[F].delay(pool))(cp => Sync[F].delay(cp.dispose()))
      xa = Transactor.fromDataSource[F](ds, ec)
    } yield new ActivityLogDb[F](xa)
  }
}
