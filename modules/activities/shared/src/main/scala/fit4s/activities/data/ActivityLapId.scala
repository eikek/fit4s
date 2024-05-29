package fit4s.activities.data

import cats.Eq
import cats.Show

import io.bullet.borer.*

final class ActivityLapId(val id: Long) extends AnyVal:
  override def toString = s"ActivityLap($id)"

object ActivityLapId:
  def apply(id: Long): ActivityLapId = new ActivityLapId(id)

  implicit val ordering: Ordering[ActivityLapId] =
    Ordering.by(_.id)

  implicit val eq: Eq[ActivityLapId] =
    Eq.by(_.id)

  implicit val jsonEncoder: Encoder[ActivityLapId] =
    Encoder.forLong.contramap(_.id)

  implicit val jsonDecoder: Decoder[ActivityLapId] =
    Decoder.forLong.map(apply)

  given Show[ActivityLapId] = Show.show(_.id.toString)
