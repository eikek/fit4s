package fit4s.strava.impl

import cats.effect._
import cats.syntax.all._
import fit4s.strava.StravaClientConfig
import fit4s.strava.data._
import fs2.Stream
import fs2.io.file.Path
import io.circe.Json
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.{`Content-Type`, `User-Agent`}
import org.http4s.multipart.{Multiparts, Part}
import org.http4s.{MediaType, Method, ProductId}

import scala.concurrent.duration._

final class StravaUpload[F[_]: Async](config: StravaClientConfig, client: Client[F])
    extends Http4sClientDsl[F] {

  def uploadFile(
      accessToken: StravaAccessToken,
      externalId: Option[String],
      file: Path,
      fileType: StravaFileType,
      name: String,
      description: Option[String],
      commute: Boolean
  ): F[StravaActivityId] = {
    val uri = config.apiUrl / "uploads"

    for {
      mps <- Multiparts.forSync[F]
      body <- mps.multipart(
        Vector(
          Part.formData[F]("name", name).some,
          description.map(d => Part.formData[F]("description", d)),
          Option.when(commute)(Part.formData[F]("commute", "1")),
          Part.formData[F]("data_type", fileType.name).some,
          externalId.map(id => Part.formData[F]("external_id", id)),
          Part
            .fileData(
              "file",
              file,
              `Content-Type`(MediaType.application.`octet-stream`)
            )
            .some
        ).flatten
      )

      req = Method
        .POST(body, uri)
        .putAuth(accessToken)
        .putHeaders(
          `Content-Type`(
            MediaType.multipart.`form-data`
              .withExtensions(Map("boundary" -> body.boundary.value))
          ),
          `User-Agent`(ProductId("fit4s", Some("0.1.0")))
        )

      upload <- client.expectOr[StravaUploadStatus](req)(resp =>
        resp
          .as[Json]
          .map(body =>
            new Exception(
              s"Unexpected response uploading ${file.absolute}: ${resp.status}\n${body.spaces2}"
            )
          )
      )

      result <- waitForActivityId(accessToken, upload).compile.lastOrError
      id <- result.activityId match {
        case Some(aid) => aid.pure[F]
        case None =>
          result.error match {
            case Some(msg) =>
              Async[F].raiseError(
                new Exception(
                  s"There was an error uploading: $msg. File: ${file.absolute}"
                )
              )
            case None =>
              Async[F].raiseError(
                new Exception(
                  s"No activity id could be obtain from upload: ${file.absolute}"
                )
              )
          }
      }
    } yield id
  }

  private def waitForActivityId(
      accessToken: StravaAccessToken,
      uploadStatus: StravaUploadStatus
  ): Stream[F, StravaUploadStatus] =
    (Stream.emit(uploadStatus) ++ Stream
      .awakeEvery(1.1.seconds)
      .flatMap(_ => Stream.eval(getUploadStatus(accessToken, uploadStatus.id)))
      .repeat)
      .takeThrough(s => s.activityId.isEmpty && s.error.isEmpty)
      .take(15)

  def getUploadStatus(
      accessToken: StravaAccessToken,
      uploadId: StravaUploadId
  ): F[StravaUploadStatus] = {
    val uri = config.apiUrl / "uploads" / uploadId

    client.expect[StravaUploadStatus](
      Method.GET(uri).putAuth(accessToken)
    )
  }
}
