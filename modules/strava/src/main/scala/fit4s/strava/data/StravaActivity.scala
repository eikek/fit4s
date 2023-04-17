package fit4s.strava.data

import cats.syntax.all._
import fit4s.data.{Distance, Position, Semicircle}
import fit4s.profile.types.Sport
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

import java.time.Instant

final case class StravaActivity(
    name: String,
    distance: Distance,
    sportType: String,
    id: StravaActivityId,
    startDate: Instant,
    trainer: Boolean,
    commute: Boolean,
    gearId: Option[String],
    startLatlng: Option[Position],
    endLatlng: Option[Position],
    externalId: String
) {
  lazy val stravaSport: Option[StravaSportType] =
    StravaSportType.fromString(sportType).toOption

  lazy val fitSport: Option[Sport] =
    stravaSport.flatMap(_.fitSport)
}

object StravaActivity {
  implicit val jsonDecoder: Decoder[StravaActivity] =
    JsonCodec.activityDecoder

  private object JsonCodec {
    implicit val configuration: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    implicit val instantDecoder: Decoder[Instant] =
      Decoder.decodeInstant

    implicit val distanceDecoder: Decoder[Distance] =
      Decoder.decodeDouble.map(Distance.meter)

    implicit val positionDecoder: Decoder[Option[Position]] =
      Decoder[Option[List[Double]]].emap {
        case Some(lat :: lng :: Nil) =>
          Right(Position(Semicircle.degree(lat), Semicircle.degree(lng)).some)

        case Some(_) => Right(None)

        case None => Right(None)
      }

    val activityDecoder: Decoder[StravaActivity] =
      deriveConfiguredDecoder
  }
}
