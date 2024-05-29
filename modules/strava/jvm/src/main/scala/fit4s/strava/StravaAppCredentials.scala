package fit4s.strava

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class StravaAppCredentials(
    clientId: String,
    clientSecret: String
)

object StravaAppCredentials:

  given Encoder[StravaAppCredentials] = deriveEncoder
