package fit4s.activities.data

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class StravaGear(
    id: String,
    primary: Boolean,
    name: String
)

object StravaGear {
  implicit val jsonDecoder: Decoder[StravaGear] =
    deriveDecoder
}
