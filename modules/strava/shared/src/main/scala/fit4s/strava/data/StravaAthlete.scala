package fit4s.strava.data

import io.bullet.borer.NullOptions.given
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs

final case class StravaAthlete(
    id: StravaAthleteId,
    username: Option[String],
    bikes: List[StravaGear],
    shoes: List[StravaGear]
)

object StravaAthlete {
  implicit val jsonDecoder: Decoder[StravaAthlete] =
    MapBasedCodecs.deriveDecoder
}
