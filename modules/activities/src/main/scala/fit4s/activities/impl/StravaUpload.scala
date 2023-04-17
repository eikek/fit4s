package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import fs2.io.file.Path

import fit4s.activities.data._
import fit4s.strava.StravaClient
import fit4s.strava.data.{StravaAccessToken, StravaActivityId, StravaFileType}

import org.http4s.client.dsl.Http4sClientDsl

final class StravaUpload[F[_]: Async](
    stravaClient: StravaClient[F],
    getToken: F[StravaAccessToken]
) extends Http4sClientDsl[F] {

  def uploadFit(
      activityId: ActivityId,
      fitFile: Path,
      name: String,
      description: Option[String],
      commute: Boolean
  ): F[StravaActivityId] = {
    val externalId = Some(s"fit4s_${activityId.id}")
    val dataType =
      if (fitFile.extName == ".gz") StravaFileType.FitGz else StravaFileType.Fit

    getToken.flatMap(token =>
      stravaClient.uploadFile(
        accessToken = token,
        externalId = externalId,
        file = fitFile,
        fileType = dataType,
        name = name,
        description = description,
        commute = commute
      )
    )
  }
}
