package fit4s.strava.data

import java.time.Instant

import cats.syntax.all.*

import fit4s.common.borer.syntax.all.*
import fit4s.data.{Distance, Position, Semicircle}
import fit4s.profile.types.Sport

import io.bullet.borer.NullOptions._
import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

final case class StravaActivity(
    name: String,
    distance: Distance,
    sport_type: String,
    id: StravaActivityId,
    start_date: Instant,
    trainer: Boolean,
    commute: Boolean,
    gear_id: Option[String],
    start_latlng: Option[Position],
    end_latlng: Option[Position],
    external_id: String
) {
  lazy val stravaSport: Option[StravaSportType] =
    StravaSportType.fromString(sport_type).toOption

  lazy val fitSport: Option[Sport] =
    stravaSport.flatMap(_.fitSport)
}

object StravaActivity {
  implicit val jsonDecoder: Decoder[StravaActivity] =
    JsonCodec.activityDecoder

  private object JsonCodec {

    implicit val instantDecoder: Decoder[Instant] =
      Decoder.forString.emap(s =>
        Either.catchNonFatal(Instant.parse(s)).leftMap(_.getMessage)
      )

    implicit val distanceDecoder: Decoder[Distance] =
      Decoder.forDouble.map(Distance.meter)

    implicit val positionDecoder: Decoder[Option[Position]] =
      Decoder[Option[List[Double]]].emap {
        case Some(lat :: lng :: Nil) =>
          Right(Position(Semicircle.degree(lat), Semicircle.degree(lng)).some)

        case Some(_) => Right(None)

        case None => Right(None)
      }

    val activityDecoder: Decoder[StravaActivity] =
      deriveDecoder
  }
}
