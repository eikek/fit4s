package fit4s.activities

import cats.effect._
import cats.syntax.all._
import doobie.util.transactor.Transactor
import fit4s.activities.data.{ActivityId, TagName}
import fit4s.activities.impl.{GeoLookupDb, GeoPlaceAttach, StravaImpl, StravaOAuth}
import fit4s.activities.records.RStravaToken
import fit4s.geocode.ReverseLookup
import fs2.Stream
import fs2.io.file.Path
import org.http4s.client.Client

import java.time.ZoneId
import scala.concurrent.duration.FiniteDuration

trait StravaSupport[F[_]] {

  def initOAuth(cfg: StravaOAuthConfig, timeout: FiniteDuration): F[Option[RStravaToken]]

  def deleteTokens: F[Int]

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
      reverseLookup: ReverseLookup[F],
      xa: Transactor[F],
      client: Client[F]
  ): F[StravaSupport[F]] =
    for {
      lookup <- GeoLookupDb(reverseLookup, xa)
      oauth = new StravaOAuth[F](client, xa)
      strava = new StravaImpl[F](zoneId, oauth, xa, new GeoPlaceAttach[F](xa, lookup))
    } yield strava
}
