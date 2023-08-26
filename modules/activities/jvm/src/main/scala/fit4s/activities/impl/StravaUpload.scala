package fit4s.activities.impl

import scala.concurrent.duration.FiniteDuration

import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.activities.StravaExternalId
import fit4s.activities.data.*
import fit4s.data.FileId
import fit4s.strava.StravaClient
import fit4s.strava.data.*

import org.http4s.client.dsl.Http4sClientDsl

final class StravaUpload[F[_]: Async](
    stravaClient: StravaClient[F],
    getToken: F[StravaAccessToken]
) extends Http4sClientDsl[F] {

  def uploadFit(
      activityId: ActivityId,
      fileId: FileId,
      fitFile: Path,
      name: String,
      description: Option[String],
      commute: Boolean,
      timeout: FiniteDuration,
      waitingCallback: (FiniteDuration, Int) => F[Unit]
  ): F[Either[StravaUploadError, StravaActivityId]] = {
    val externalId = StravaExternalId(activityId, fileId)
    val dataType =
      if (fitFile.extName == ".gz") StravaFileType.FitGz else StravaFileType.Fit

    getToken.flatMap(token =>
      stravaClient.uploadFile(
        accessToken = token,
        externalId = externalId.asString.some,
        file = fitFile,
        fileType = dataType,
        name = name,
        description = description,
        commute = commute,
        processingTimeout = timeout,
        waitingCallback = waitingCallback
      )
    )
  }
}
