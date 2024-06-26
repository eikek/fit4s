package fit4s.strava.data

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class TokenAndScope(
    tokenResponse: StravaTokenResponse,
    scope: StravaScope
)

object TokenAndScope:
  implicit val jsonDecoder: Decoder[TokenAndScope] =
    deriveDecoder

  implicit val jsonEncoder: Encoder[TokenAndScope] =
    deriveEncoder
