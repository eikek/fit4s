package fit4s.strava.data

import fit4s.common.borer.{EncoderHelper, JsonValue}

import io.bullet.borer.NullOptions.given
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs

final case class StravaUpdatableActivity(
    commute: Option[Boolean],
    trainer: Option[Boolean],
    description: Option[String],
    name: Option[String],
    gear_id: Option[String]
):
  def isEmpty: Boolean =
    commute.isEmpty && trainer.isEmpty && description.isEmpty &&
      name.isEmpty && gear_id.isEmpty

object StravaUpdatableActivity:
  val empty: StravaUpdatableActivity =
    StravaUpdatableActivity(None, None, None, None, None)

  implicit val jsonEncoder: Encoder[StravaUpdatableActivity] =
    EncoderHelper.from[StravaUpdatableActivity] { a =>
      List(
        "commute" -> a.commute.map(JsonValue.apply),
        "trainer" -> a.trainer.map(JsonValue.apply),
        "description" -> a.description.map(JsonValue.apply),
        "name" -> a.name.map(JsonValue.apply),
        "gear_id" -> a.gear_id.map(JsonValue.apply)
      )
    }

  implicit val jsonDecoder: Decoder[StravaUpdatableActivity] =
    MapBasedCodecs.deriveDecoder
