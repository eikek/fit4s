package fit4s.strava.data

import io.bullet.borer.NullOptions.given
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs

final case class StravaUploadStatus(
    id: StravaUploadId,
    external_id: Option[String],
    error: Option[String],
    status: String,
    activity_id: Option[StravaActivityId]
)

object StravaUploadStatus:
  implicit val jsonDecoder: Decoder[StravaUploadStatus] =
    JsonCodec.decoder

  implicit val jsonEncoder: Encoder[StravaUploadStatus] =
    JsonCodec.encoder

  private object JsonCodec:
    val decoder: Decoder[StravaUploadStatus] =
      MapBasedCodecs.deriveDecoder

    val encoder: Encoder[StravaUploadStatus] =
      MapBasedCodecs.deriveEncoder
