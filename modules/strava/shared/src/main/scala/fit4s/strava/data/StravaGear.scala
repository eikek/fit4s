package fit4s.strava.data

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class StravaGear(
    id: String,
    primary: Boolean,
    name: String
)

object StravaGear:
  implicit val jsonDecoder: Decoder[StravaGear] =
    deriveDecoder
