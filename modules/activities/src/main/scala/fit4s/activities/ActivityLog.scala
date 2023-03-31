package fit4s.activities

import cats.effect._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import fit4s.activities.data.{ActivityId, TagId, TagName}
import fit4s.activities.impl.ActivityLogDb
import fit4s.activities.records.{ActivityRecord, TagRecord}
import fs2._
import fs2.io.file.Path
import org.h2.jdbcx.JdbcConnectionPool

trait ActivityLog[F[_]] {

  def initialize: F[Unit]

  def importFromDirectories(tagged: Set[TagName]): Pipe[F, Path, InsertResult]

  def deleteActivities(query: Query): F[Int]

  def linkTag(tagId: TagId, activityId: ActivityId): F[InsertResult]

  def unlinkTag(tagId: TagId, activityId: ActivityId): F[Int]

  def activityTags(activityId: ActivityId): Stream[F, TagRecord]

  def activityList(query: Query): Stream[F, ActivityRecord]

  def activityStats(query: Query): F[ActivityStats]

  def tagRepository: TagRepo[F]

  def locationRepository: LocationRepo[F]
}

object ActivityLog {
  def apply[F[_]: Async](jdbcConfig: JdbcConfig): Resource[F, ActivityLog[F]] = {
    val pool = JdbcConnectionPool.create(
      jdbcConfig.url,
      jdbcConfig.user,
      jdbcConfig.password
    )

    for {
      ec <- ExecutionContexts.fixedThreadPool(10)
      ds <- Resource.make(Sync[F].delay(pool))(cp => Sync[F].delay(cp.dispose()))
      xa = Transactor.fromDataSource[F](ds, ec)
    } yield new ActivityLogDb[F](jdbcConfig, xa)
  }

  def default[F[_]: Async]: Resource[F, ActivityLog[F]] =
    Resource.eval(JdbcConfig.defaultFilesystem[F]).flatMap(apply[F])
}
