package fit4s.activities

import cats.effect._
import cats.syntax.all._
import doobie.util.transactor.Transactor
import fit4s.activities.data.{ActivityId, StravaActivity, StravaGear, TagName}
import fit4s.activities.impl.{Cache, GeoLookupDb, GeoPlaceAttach, StravaImpl, StravaOAuth}
import fit4s.activities.records.RStravaToken
import fit4s.geocode.ReverseLookup
import fs2.Stream
import fs2.io.file.Path
import org.http4s.client.Client

import java.time.{Instant, ZoneId}
import scala.concurrent.duration.FiniteDuration

trait StravaSupport[F[_]] {

  def initOAuth(cfg: StravaAuthConfig, timeout: FiniteDuration): F[Option[RStravaToken]]

  def deleteTokens: F[Int]

  def listActivities(
      cfg: StravaAuthConfig,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]]

  final def listAllActivities(
      cfg: StravaAuthConfig,
      after: Instant,
      before: Instant
  ): Stream[F, StravaActivity] =
    Stream
      .iterate(1)(_ + 1)
      .evalMap(page => listActivities(cfg, after, before, page, 150))
      // strava docs say that pages could be less than per_page, so check for empty result
      .takeWhile(_.nonEmpty)
      .flatMap(Stream.emits)

  def findGear(cfg: StravaAuthConfig, gearId: String): F[Option[StravaGear]]

  def loadExport(
      stravaExport: Path,
      tagged: Set[TagName],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]
}

object StravaSupport {

  def apply[F[_]: Async](
      zoneId: ZoneId,
      stravaCfg: StravaConfig,
      reverseLookup: ReverseLookup[F],
      xa: Transactor[F],
      client: Client[F]
  ): F[StravaSupport[F]] =
    for {
      lookup <- GeoLookupDb(reverseLookup, xa)
      oauth = new StravaOAuth[F](stravaCfg, client, xa)
      gearCache <- Cache.memory[F, String, StravaGear](stravaCfg.gearCacheSize)
      strava = new StravaImpl[F](
        zoneId,
        stravaCfg,
        client,
        oauth,
        xa,
        new GeoPlaceAttach[F](xa, lookup),
        gearCache
      )
    } yield strava
}
