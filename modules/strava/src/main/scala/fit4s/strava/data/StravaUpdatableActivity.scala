package fit4s.strava.data

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

final case class StravaUpdatableActivity(
    commute: Option[Boolean],
    trainer: Option[Boolean],
    description: Option[String],
    name: Option[String],
    gearId: Option[String]
) {
  def isEmpty: Boolean =
    commute.isEmpty && trainer.isEmpty && description.isEmpty &&
      name.isEmpty && gearId.isEmpty
}

object StravaUpdatableActivity {
  val empty: StravaUpdatableActivity =
    StravaUpdatableActivity(None, None, None, None, None)

  implicit val jsonEncoder: Encoder[StravaUpdatableActivity] =
    JsonCodec.encoder

  implicit val jsonDecoder: Decoder[StravaUpdatableActivity] =
    JsonCodec.decoder

  private object JsonCodec {
    implicit val config: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    val decoder: Decoder[StravaUpdatableActivity] =
      deriveConfiguredDecoder

    val encoder: Encoder[StravaUpdatableActivity] =
      deriveConfiguredEncoder[StravaUpdatableActivity].mapJson(_.deepDropNullValues)
  }
}
