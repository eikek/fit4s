package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import fit4s.activities.StravaConfig
import fit4s.activities.data._
import fit4s.activities.records.RStravaToken
import fs2.Stream
import fs2.io.file.Path
import io.circe.Json
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers._
import org.http4s.multipart.{Multiparts, Part}

import scala.concurrent.duration._

final class StravaUpload[F[_]: Async](
    client: Client[F],
    config: StravaConfig,
    getToken: F[RStravaToken]
) extends Http4sClientDsl[F] {

  def uploadFit(
      activityId: ActivityId,
      fitFile: Path,
      name: String,
      description: Option[String],
      commute: Boolean
  ): F[StravaExternalId] =
    for {
      token <- getToken
      credentials = Credentials.Token(AuthScheme.Bearer, token.accessToken)

      uri = config.apiUrl / "uploads"

      dataType =
        if (fitFile.extName == ".gz") "fit.gz" else "fit"

      mps <- Multiparts.forSync[F]
      body <- mps.multipart(
        Vector(
          Part.formData[F]("name", name).some,
          description.map(d => Part.formData[F]("description", d)),
          Option.when(commute)(Part.formData[F]("commute", "1")),
          Part.formData[F]("data_type", dataType).some,
          Part.formData[F]("external_id", s"fit4s_${activityId.id}").some,
          Part
            .fileData(
              "file",
              fitFile,
              `Content-Type`(MediaType.application.`octet-stream`)
            )
            .some
        ).flatten
      )

      req = Method
        .POST(body, uri)
        .withHeaders(
          Authorization(credentials),
          `Content-Type`(
            MediaType.multipart.`form-data`
              .withExtensions(Map("boundary" -> body.boundary.value))
          ),
          `User-Agent`(ProductId("fit4s", Some("0.0.1")))
        )

      upload <- client.expectOr[StravaUploadStatus](req)(resp =>
        resp
          .as[Json]
          .map(body =>
            new Exception(
              s"Unexpected response uploading ${fitFile.absolute}: ${resp.status}\n${body.spaces2}"
            )
          )
      )

      result <- waitForActivityId(upload).compile.lastOrError
      id <- result.activityId match {
        case Some(aid) => aid.pure[F]
        case None =>
          result.error match {
            case Some(msg) =>
              Async[F].raiseError(
                new Exception(
                  s"There was an error uploading: $msg. File: ${fitFile.absolute}"
                )
              )
            case None =>
              Async[F].raiseError(
                new Exception(
                  s"No activity id could be obtain from upload: ${fitFile.absolute}"
                )
              )
          }
      }
    } yield id

  private def waitForActivityId(
      uploadStatus: StravaUploadStatus
  ): Stream[F, StravaUploadStatus] =
    (Stream.emit(uploadStatus) ++ Stream
      .awakeEvery(1.1.seconds)
      .flatMap(_ => Stream.eval(getUploadStatus(uploadStatus.id)))
      .repeat)
      .takeThrough(s => s.activityId.isEmpty && s.error.isEmpty)
      .take(15)

  def getUploadStatus(uploadId: StravaUploadId): F[StravaUploadStatus] =
    for {
      token <- getToken
      credentials = Credentials.Token(AuthScheme.Bearer, token.accessToken)

      uri = config.apiUrl / "uploads" / uploadId
      result <- client.expect[StravaUploadStatus](
        Method.GET(uri).withHeaders(Authorization(credentials))
      )

    } yield result

  def updateActivity(id: StravaExternalId, data: StravaUpdatableActivity): F[Unit] =
    for {
      token <- getToken
      credentials = Credentials.Token(AuthScheme.Bearer, token.accessToken)

      uri = config.apiUrl / "activities" / id
      result <- client.successful(
        Method.PUT(data, uri).withHeaders(Authorization(credentials))
      )
      _ <-
        if (result) ().pure[F]
        else
          Async[F].raiseError(
            new Exception(s"Activity $id update returned with a failure")
          )
    } yield ()
}
