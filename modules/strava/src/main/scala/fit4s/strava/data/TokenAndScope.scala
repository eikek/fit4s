package fit4s.strava.data

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

final case class TokenAndScope(
    tokenResponse: StravaTokenResponse,
    scope: StravaScope
)

object TokenAndScope {
  implicit val jsonDecoder: Decoder[TokenAndScope] =
    deriveDecoder

  implicit val jsonEncoder: Encoder[TokenAndScope] =
    deriveEncoder
}
