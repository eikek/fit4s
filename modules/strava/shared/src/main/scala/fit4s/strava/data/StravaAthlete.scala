package fit4s.strava.data

import io.bullet.borer.NullOptions.*
import io.bullet.borer.NullOptions._
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

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
