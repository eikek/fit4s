package fit4s.strava.impl

import java.time.Instant

import scala.concurrent.duration.FiniteDuration

import cats.effect._
import cats.syntax.all._
import fs2.io.file.Path

import fit4s.strava.data._
import fit4s.strava.{StravaAppCredentials, StravaClient, StravaClientConfig}

import org.http4s.Method
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl

final class DefaultStravaClient[F[_]: Async](
    client: Client[F],
    config: StravaClientConfig,
    gearCache: Cache[F, String, StravaGear]
) extends StravaClient[F]
    with Http4sClientDsl[F] {

  private[this] val oauth = new StravaOAuth[F](config, client)
  private[this] val upload = new StravaUpload[F](config, client)

  def initAuth(cfg: StravaAppCredentials, timeout: FiniteDuration) =
    oauth.init(cfg, timeout)

  def refreshAuth(
      cfg: StravaAppCredentials,
      latestToken: F[Option[TokenAndScope]]
  ) = oauth.refresh(cfg, latestToken)

  def getAthlete(accessToken: StravaAccessToken): F[StravaAthlete] =
    client.expect[StravaAthlete](
      Method.GET(config.apiUrl / "athlete").putAuth(accessToken)
    )

  def listActivities(
      accessToken: StravaAccessToken,
      after: Instant,
      before: Instant,
      page: Int,
      perPage: Int
  ): F[List[StravaActivity]] = {
    val uri = (config.apiUrl / "athlete" / "activities")
      .withQueryParam("before", before.getEpochSecond)
      .withQueryParam("after", after.getEpochSecond)
      .withQueryParam("page", page)
      .withQueryParam("per_page", perPage)

    client.expect[List[StravaActivity]](
      Method.GET(uri).putAuth(accessToken)
    )
  }

  def findGear(accessToken: StravaAccessToken, gearId: String) =
    gearCache.cached(findGear1(accessToken, _))(gearId)

  private def findGear1(
      accessToken: StravaAccessToken,
      gearId: String
  ): F[Option[StravaGear]] = {
    val uri = config.apiUrl / "gear" / gearId
    client.expectOption[StravaGear](
      Method.GET(uri).putAuth(accessToken)
    )
  }

  def updateActivity(
      accessToken: StravaAccessToken,
      id: StravaActivityId,
      data: StravaUpdatableActivity
  ) = {
    val uri = config.apiUrl / "activities" / id
    client
      .successful(
        Method.PUT(data, uri).putAuth(accessToken)
      )
      .flatMap {
        case true => ().pure[F]
        case false =>
          Async[F].raiseError(
            new Exception(s"Activity $id update returned with a failure")
          )
      }
  }

  def uploadFile(
      accessToken: StravaAccessToken,
      externalId: Option[String],
      file: Path,
      fileType: StravaFileType,
      name: String,
      description: Option[String],
      commute: Boolean
  ): F[StravaActivityId] =
    upload.uploadFile(accessToken, externalId, file, fileType, name, description, commute)
}
