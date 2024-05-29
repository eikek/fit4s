package fit4s.strava.data

import cats.Show

import io.bullet.borer.*

final class StravaTokenId(val id: Long) extends AnyVal:
  override def toString = s"StravaTokenId($id)"

object StravaTokenId:
  def apply(id: Long): StravaTokenId = new StravaTokenId(id)

  implicit val jsonEncoder: Encoder[StravaTokenId] =
    Encoder.forLong.contramap(_.id)

  implicit val jsonDecoder: Decoder[StravaTokenId] =
    Decoder.forLong.map(apply)

  given Show[StravaTokenId] = Show.show(_.id.toString)
