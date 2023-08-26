package fit4s.strava.data

import io.bullet.borer._

final class StravaAccessToken(val token: String) extends AnyVal {
  override def toString = "StravaAccessToken(***)"
}

object StravaAccessToken {
  def apply(token: String): StravaAccessToken = new StravaAccessToken(token)

  implicit val jsonEncoder: Encoder[StravaAccessToken] =
    Encoder.forString.contramap(_.token)

  implicit val jsonDecoder: Decoder[StravaAccessToken] =
    Decoder.forString.map(apply)
}
