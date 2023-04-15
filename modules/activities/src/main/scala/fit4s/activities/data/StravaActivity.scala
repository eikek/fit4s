package fit4s.activities.data

import fit4s.data.{Distance, Position, Semicircle}
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import java.time.Instant

final case class StravaActivity(
    name: String,
    distance: Distance,
    sportType: String,
    id: StravaExternalId,
    uploadId: Long,
    startDate: Instant,
    trainer: Boolean,
    commute: Boolean,
    gearId: String,
    startLatlng: Position,
    endLatlng: Position,
    hasHeartrate: Boolean,
    externalId: String
)

object StravaActivity {
  implicit val jsonDecoder: Decoder[StravaActivity] =
    JsonCodec.activityDecoder

  private object JsonCodec {
    implicit val configuration: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    implicit val instantDecoder: Decoder[Instant] =
      Decoder.decodeInstant

    implicit val stravaIdDecoder: Decoder[StravaExternalId] =
      Decoder.decodeLong.map(StravaExternalId.apply)

    implicit val distanceDecoder: Decoder[Distance] =
      Decoder.decodeDouble.map(Distance.meter)

    implicit val positionDecoder: Decoder[Position] =
      Decoder[List[Double]].emap {
        case lat :: lng :: Nil =>
          Right(Position(Semicircle.degree(lat), Semicircle.degree(lng)))
        case other =>
          Left(s"Invalid position in strava activity: $other")
      }

    val activityDecoder: Decoder[StravaActivity] =
      deriveConfiguredDecoder
  }
}
