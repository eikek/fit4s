package fit4s.strava.data

import fs2.io.file.Path

import io.bullet.borer.NullOptions._
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

final case class StravaUploadStatus(
    id: StravaUploadId,
    external_id: Option[String],
    error: Option[String],
    status: String,
    activity_id: Option[StravaActivityId]
)

object StravaUploadStatus {
  implicit val jsonDecoder: Decoder[StravaUploadStatus] =
    JsonCodec.decoder

  implicit val jsonEncoder: Encoder[StravaUploadStatus] =
    JsonCodec.encoder

  private object JsonCodec {
    val decoder: Decoder[StravaUploadStatus] =
      deriveDecoder

    val encoder: Encoder[StravaUploadStatus] =
      deriveEncoder
  }
}
