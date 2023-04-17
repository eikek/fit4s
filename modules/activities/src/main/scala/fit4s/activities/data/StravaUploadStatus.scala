package fit4s.activities.data

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

final case class StravaUploadStatus(
    id: StravaUploadId,
    externalId: Option[String],
    error: Option[String],
    status: String,
    activityId: Option[StravaActivityId]
)

object StravaUploadStatus {
  implicit val jsonDecoder: Decoder[StravaUploadStatus] =
    JsonCodec.decoder

  private object JsonCodec {
    implicit val configuration: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    val decoder: Decoder[StravaUploadStatus] =
      deriveConfiguredDecoder
  }
}
