package fit4s.strava.data

import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

final case class StravaGear(
    id: String,
    primary: Boolean,
    name: String
)

object StravaGear {
  implicit val jsonDecoder: Decoder[StravaGear] =
    deriveDecoder
}
