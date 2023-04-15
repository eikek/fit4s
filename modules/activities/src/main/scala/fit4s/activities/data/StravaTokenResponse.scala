package fit4s.activities.data

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Json}

import java.time.{Duration, Instant}

final case class StravaTokenResponse(
    token_type: String,
    access_token: String,
    expires_at: Long,
    expires_in: Long,
    refresh_token: String,
    athlete: Option[Json]
) {
  val expiresAt: Instant = Instant.ofEpochSecond(expires_at)
  val expiresIn: Duration = Duration.ofSeconds(expires_in)
}

object StravaTokenResponse {

  implicit val jsonDecoder: Decoder[StravaTokenResponse] =
    deriveDecoder
}
