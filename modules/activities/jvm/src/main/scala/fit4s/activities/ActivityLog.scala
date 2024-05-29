package fit4s.activities

import java.time.ZoneId

import scala.concurrent.duration.*

import cats.data.NonEmptyList
import cats.effect.*
import fs2.*
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import fs2.io.net.Network

import fit4s.activities.data.*
import fit4s.activities.dump.ExportData
import fit4s.activities.impl.{ActivityLogDb, GeoLookupDb}
import fit4s.geocode.{NominatimConfig, ReverseLookup}
import fit4s.strava.StravaClientConfig

import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.h2.jdbcx.JdbcConnectionPool
import org.http4s.ember.client.EmberClientBuilder
import org.postgresql.ds.PGConnectionPoolDataSource

trait ActivityLog[F[_]]:
  def initialize: F[Unit]

  def geoLookup(ids: List[ActivityId], onId: ActivityId => F[Unit]): F[Unit]

  def importFromDirectories(
      zoneId: ZoneId,
      tagged: Set[TagName],
      callback: ImportCallback[F],
      dirs: NonEmptyList[Path],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]

  def syncNewFiles(
      zoneId: ZoneId,
      tagged: Set[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]

  def activityList(query: ActivityQuery): Stream[F, ActivityListResult]

  def activitySummary(
      query: ActivityQuery
  ): F[Vector[ActivitySessionSummary]]

  def activityDetails(
      id: ActivityId,
      withSessionData: Boolean
  ): F[Option[ActivityDetailResult]]

  def deleteActivities(ids: NonEmptyList[ActivityId], hardDelete: Boolean): F[Int]

  def setActivityName(id: ActivityId, name: String): F[Unit]

  def setGeneratedActivityName(id: ActivityId, zoneId: ZoneId): F[Unit]

  def setActivityNotes(id: ActivityId, notes: Option[String]): F[Unit]

  def tagRepository: TagRepo[F]

  def locationRepository: LocationRepo[F]

  def strava: StravaSupport[F]

  def exportData: ExportData[F]

object ActivityLog:
  def apply[F[_]: Async: Network: Files: Compression](
      jdbcConfig: JdbcConfig,
      nominatimCfg: NominatimConfig,
      stravaConfig: StravaClientConfig,
      httpTimeout: Duration
  ): Resource[F, ActivityLog[F]] =
    val pool =
      jdbcConfig.dbms match
        case JdbcConfig.Dbms.H2 =>
          JdbcConnectionPool.create(
            jdbcConfig.url,
            jdbcConfig.user,
            jdbcConfig.password
          )
        case JdbcConfig.Dbms.Postgres =>
          val ds = new PGConnectionPoolDataSource()
          ds.setURL(jdbcConfig.url)
          ds.setUser(jdbcConfig.user)
          ds.setPassword(jdbcConfig.password)
          JdbcConnectionPool.create(ds)

    for {
      ec <- ExecutionContexts.fixedThreadPool(10)
      ds <- Resource.make(Sync[F].delay(pool))(cp => Sync[F].delay(cp.dispose()))
      xa = Transactor.fromDataSource[F](ds, ec)
      client <- EmberClientBuilder.default[F].withTimeout(httpTimeout).build
      revLookup <- Resource.eval(ReverseLookup.osm[F](client, nominatimCfg))
      strava <- Resource.eval(
        StravaSupport[F](stravaConfig, revLookup, xa, client)
      )
      geoLookup <- Resource.eval(GeoLookupDb(revLookup, xa))
    } yield new ActivityLogDb[F](jdbcConfig, xa, geoLookup, strava)
