package fit4s.activities

import cats.data.NonEmptyList
import cats.effect._
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import fit4s.activities.data._
import fit4s.activities.impl.{ActivityLogDb, GeoLookupDb}
import fit4s.geocode.{NominatimConfig, ReverseLookup}
import fit4s.strava.StravaClientConfig
import fs2._
import fs2.io.file.Path
import org.h2.jdbcx.JdbcConnectionPool
import org.http4s.ember.client.EmberClientBuilder

import java.time.ZoneId

trait ActivityLog[F[_]] {

  def initialize: F[Unit]

  def importFromDirectories(
      tagged: Set[TagName],
      callback: ImportCallback[F],
      dirs: NonEmptyList[Path],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]

  def syncNewFiles(
      tagged: Set[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]

  def activityList(query: ActivityQuery): Stream[F, ActivityListResult]

  def activitySummary(
      query: ActivityQuery
  ): F[Vector[ActivitySessionSummary]]

  def activityDetails(id: ActivityId): F[Option[ActivityDetailResult]]

  def deleteActivities(ids: NonEmptyList[ActivityId], hardDelete: Boolean): F[Int]

  def setActivityName(id: ActivityId, name: Option[String]): F[Unit]

  def setActivityNotes(id: ActivityId, notes: Option[String]): F[Unit]

  def tagRepository: TagRepo[F]

  def locationRepository: LocationRepo[F]

  def strava: StravaSupport[F]
}

object ActivityLog {
  def apply[F[_]: Async](
      jdbcConfig: JdbcConfig,
      nominatimCfg: NominatimConfig,
      stravaConfig: StravaClientConfig,
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
      client <- EmberClientBuilder.default[F].build
      revLookup <- Resource.eval(ReverseLookup.osm[F](client, nominatimCfg))
      strava <- Resource.eval(
        StravaSupport[F](zoneId, stravaConfig, revLookup, xa, client)
      )
      geoLookup <- Resource.eval(GeoLookupDb(revLookup, xa))
    } yield new ActivityLogDb[F](jdbcConfig, zoneId, xa, geoLookup, strava)
  }
}
