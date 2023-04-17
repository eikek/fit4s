package fit4s.strava.data

import io.circe.{Decoder, Encoder}

final class StravaAccessToken(val token: String) extends AnyVal {
  override def toString = "StravaAccessToken(***)"
}

object StravaAccessToken {
  def apply(token: String): StravaAccessToken = new StravaAccessToken(token)

  implicit val jsonEncoder: Encoder[StravaAccessToken] =
    Encoder.encodeString.contramap(_.token)

  implicit val jsonDecoder: Decoder[StravaAccessToken] =
    Decoder.decodeString.map(apply)
}
