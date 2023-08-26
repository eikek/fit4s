package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class ActivitySessionId(val id: Long) extends AnyVal {
  override def toString = s"ActivitySessionId($id)"
}

object ActivitySessionId {
  def apply(id: Long): ActivitySessionId = new ActivitySessionId(id)

  implicit val ordering: Ordering[ActivitySessionId] =
    Ordering.by(_.id)

  implicit val jsonDecoder: Decoder[ActivitySessionId] =
    Decoder.forLong.map(ActivitySessionId.apply)

  implicit val jsonEncoder: Encoder[ActivitySessionId] =
    Encoder.forLong.contramap(_.id)

  given Show[ActivitySessionId] = Show.show(_.id.toString)
}
