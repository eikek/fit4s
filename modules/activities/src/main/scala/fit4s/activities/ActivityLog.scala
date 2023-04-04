package fit4s.activities

import cats.data.NonEmptyList
import cats.effect._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import fit4s.activities.data.{ActivityId, ActivitySessionSummary, TagId, TagName}
import fit4s.activities.impl.ActivityLogDb
import fit4s.activities.records.{ActivitySessionRecord, TagRecord}
import fs2._
import fs2.io.file.Path
import org.h2.jdbcx.JdbcConnectionPool

import java.time.ZoneId

trait ActivityLog[F[_]] {

  def initialize: F[Unit]

  def importFromDirectories(
      tagged: Set[TagName],
      callback: ImportCallback[F],
      dirs: NonEmptyList[Path],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]

  def deleteActivities(query: ActivityQuery): F[Int]

  def linkTag(tagId: TagId, activityId: ActivityId): F[Unit]

  def unlinkTag(tagId: TagId, activityId: ActivityId): F[Int]

  def activityTags(activityId: ActivityId): Stream[F, TagRecord]

  def activityList(query: ActivityQuery): Stream[F, ActivitySessionRecord]

  def activitySummary(
      query: Option[ActivityQuery.Condition]
  ): F[Vector[ActivitySessionSummary]]

  def tagRepository: TagRepo[F]

  def locationRepository: LocationRepo[F]
}

object ActivityLog {
  def apply[F[_]: Async](
      jdbcConfig: JdbcConfig,
      zoneId: ZoneId
  ): Resource[F, ActivityLog[F]] = {
    val pool = JdbcConnectionPool.create(
      jdbcConfig.url,
      jdbcConfig.user,
      jdbcConfig.password
    )

    for {
      ec <- ExecutionContexts.fixedThreadPool(10)
      ds <- Resource.make(Sync[F].delay(pool))(cp => Sync[F].delay(cp.dispose()))
      xa = Transactor.fromDataSource[F](ds, ec)
    } yield new ActivityLogDb[F](jdbcConfig, zoneId, xa)
  }

  def default[F[_]: Async](
      zoneId: ZoneId = ZoneId.systemDefault()
  ): Resource[F, ActivityLog[F]] =
    Resource.eval(JdbcConfig.defaultFilesystem[F]).flatMap(apply[F](_, zoneId))
}
