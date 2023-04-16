package fit4s.activities.data

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class StravaAthlete(
    id: StravaAthleteId,
    username: Option[String],
    bikes: List[StravaGear],
    shoes: List[StravaGear]
)

object StravaAthlete {
  implicit val jsonDecoder: Decoder[StravaAthlete] =
    deriveDecoder
}
