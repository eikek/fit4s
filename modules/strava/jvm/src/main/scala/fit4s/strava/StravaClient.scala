package fit4s.strava

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import fs2.io.net.Network
import fs2.{Chunk, Stream}

import fit4s.common.util.Cache
import fit4s.strava.data.*
import fit4s.strava.impl.DefaultStravaClient

import org.http4s.client.Client

trait StravaClient[F[_]]:
  def initAuth(
      cfg: StravaAppCredentials,
      timeout: FiniteDuration
  ): F[Option[TokenAndScope]]

  def refreshAuth(
      cfg: StravaAppCredentials,
      latestToken: F[TokenAndScope]
  ): F[TokenAndScope]

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
      .flatMap(page => Stream.chunk(Chunk.from(page)))

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
      commute: Boolean,
      processingTimeout: FiniteDuration,
      waitingCallback: (FiniteDuration, Int) => F[Unit]
  ): F[Either[StravaUploadError, StravaActivityId]]

object StravaClient:

  def apply[F[_]: Async: Network: Files](
      config: StravaClientConfig,
      client: Client[F]
  ): F[StravaClient[F]] =
    Cache
      .memory[F, String, StravaGear](config.gearCacheSize)
      .map(gearCache => new DefaultStravaClient[F](client, config, gearCache))
