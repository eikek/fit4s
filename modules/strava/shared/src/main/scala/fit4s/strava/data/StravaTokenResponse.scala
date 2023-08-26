package fit4s.strava.data

import java.time.{Duration, Instant}

import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._

final case class StravaTokenResponse(
    token_type: String,
    access_token: StravaAccessToken,
    expires_at: Instant,
    expires_in: Duration,
    refresh_token: StravaRefreshToken
)

object StravaTokenResponse {

  implicit val jsonDecoder: Decoder[StravaTokenResponse] =
    JsonCodec.decoder

  implicit val jsonEncoder: Encoder[StravaTokenResponse] =
    JsonCodec.encoder

  private object JsonCodec {
    implicit val instantDecoder: Decoder[Instant] =
      Decoder.forLong.map(Instant.ofEpochSecond)

    implicit val instantEncoder: Encoder[Instant] =
      Encoder.forLong.contramap(_.getEpochSecond)

    implicit val durationDecoder: Decoder[Duration] =
      Decoder.forLong.map(Duration.ofSeconds)

    implicit val durationEncoder: Encoder[Duration] =
      Encoder.forLong.contramap(_.toSeconds)

    val decoder: Decoder[StravaTokenResponse] =
      deriveDecoder

    val encoder: Encoder[StravaTokenResponse] =
      deriveEncoder
  }
}
