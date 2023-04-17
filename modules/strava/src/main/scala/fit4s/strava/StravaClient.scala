package fit4s.strava

import cats.effect._
import cats.syntax.all._
import fit4s.strava.data._
import fit4s.strava.impl.{Cache, DefaultStravaClient}
import fs2._
import fs2.io.file.Path
import org.http4s.client.Client

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

trait StravaClient[F[_]] {
  def initAuth(
      cfg: StravaAppCredentials,
      timeout: FiniteDuration
  ): F[Option[TokenAndScope]]

  def refreshAuth(
      cfg: StravaAppCredentials,
      latestToken: F[Option[TokenAndScope]]
  ): F[Option[TokenAndScope]]

  def getAthlete(accessToken: StravaAccessToken): F[StravaAthlete]

  def listActivities(
      accessToken: StravaAccessToken,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]]

  final def listAllActivities(
      accessToken: StravaAccessToken,
      after: Instant,
      before: Instant,
      chunkSize: Int
  ): Stream[F, StravaActivity] =
    Stream
      .iterate(1)(_ + 1)
      .evalMap(page => listActivities(accessToken, after, before, page, chunkSize))
      // strava docs say that pages could be less than per_page, so check for empty result
      .takeWhile(_.nonEmpty)
      .flatMap(page => Stream.chunk(Chunk.seq(page)))

  def findGear(accessToken: StravaAccessToken, gearId: String): F[Option[StravaGear]]

  def updateActivity(
      accessToken: StravaAccessToken,
      id: StravaActivityId,
      data: StravaUpdatableActivity
  ): F[Unit]

  def uploadFile(
      accessToken: StravaAccessToken,
      externalId: Option[String],
      file: Path,
      fileType: StravaFileType,
      name: String,
      description: Option[String],
      commute: Boolean
  ): F[StravaActivityId]
}

object StravaClient {

  def apply[F[_]: Async](
      config: StravaClientConfig,
      client: Client[F]
  ): F[StravaClient[F]] =
    Cache
      .memory[F, String, StravaGear](config.gearCacheSize)
      .map(gearCache => new DefaultStravaClient[F](client, config, gearCache))
}
