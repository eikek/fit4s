package fit4s.strava.data

import io.bullet.borer.*

final class StravaRefreshToken(val token: String) extends AnyVal:
  override def toString = "StravaRefreshToken(***)"

object StravaRefreshToken:
  def apply(token: String): StravaRefreshToken = new StravaRefreshToken(token)

  implicit val jsonEncoder: Encoder[StravaRefreshToken] =
    Encoder.forString.contramap(_.token)

  implicit val jsonDecoder: Decoder[StravaRefreshToken] =
    Decoder.forString.map(apply)
