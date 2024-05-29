package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class ActivityTagId(val id: Long) extends AnyVal:
  override def toString() = s"ActivityTagId($id)"

object ActivityTagId:
  def apply(id: Long): ActivityTagId = new ActivityTagId(id)

  implicit val ordering: Ordering[ActivityTagId] =
    Ordering.by(_.id)

  implicit val jsonDecoder: Decoder[ActivityTagId] =
    Decoder.forLong.map(apply)

  implicit val jsonEncoder: Encoder[ActivityTagId] =
    Encoder.forLong.contramap(_.id)

  given Show[ActivityTagId] = Show.show(_.id.toString)
