package fit4s.strava.data

import java.time.{Duration, Instant}

import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder, Json}

final case class StravaTokenResponse(
    tokenType: String,
    accessToken: StravaAccessToken,
    expiresAt: Instant,
    expiresIn: Duration,
    refreshToken: StravaRefreshToken,
    athlete: Option[Json]
)

object StravaTokenResponse {

  implicit val jsonDecoder: Decoder[StravaTokenResponse] =
    JsonCodec.decoder

  implicit val jsonEncoder: Encoder[StravaTokenResponse] =
    JsonCodec.encoder

  private object JsonCodec {
    implicit val config: Configuration =
      Configuration.default.withSnakeCaseMemberNames

    implicit val instantDecoder: Decoder[Instant] =
      Decoder.decodeLong.map(Instant.ofEpochSecond)

    implicit val instantEncoder: Encoder[Instant] =
      Encoder.encodeLong.contramap(_.getEpochSecond)

    implicit val durationDecoder: Decoder[Duration] =
      Decoder.decodeLong.map(Duration.ofSeconds)

    implicit val durationEncoder: Encoder[Duration] =
      Encoder.encodeLong.contramap(_.toSeconds)

    val decoder: Decoder[StravaTokenResponse] =
      deriveConfiguredDecoder

    val encoder: Encoder[StravaTokenResponse] =
      deriveConfiguredEncoder
  }
}
