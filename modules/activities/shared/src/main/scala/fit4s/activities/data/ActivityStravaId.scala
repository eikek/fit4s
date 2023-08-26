package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class ActivityStravaId(val id: Long) extends AnyVal {
  override def toString = s"ActivityStravaId($id)"
}

object ActivityStravaId {
  def apply(id: Long): ActivityStravaId = new ActivityStravaId(id)

  implicit val jsonEncoder: Encoder[ActivityStravaId] =
    Encoder.forLong.contramap(_.id)

  implicit val jsonDecoder: Decoder[ActivityStravaId] =
    Decoder.forLong.map(apply)

  given Show[ActivityStravaId] = Show.show(_.id.toString)
}
