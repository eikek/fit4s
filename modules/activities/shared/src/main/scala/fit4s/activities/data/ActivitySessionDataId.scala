package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class ActivitySessionDataId(val id: Long) extends AnyVal:
  override def toString() = s"ActivitySessionDataId($id)"

object ActivitySessionDataId:
  def apply(id: Long): ActivitySessionDataId = new ActivitySessionDataId(id)

  implicit val ordering: Ordering[ActivitySessionDataId] =
    Ordering.by(_.id)

  implicit val jsonDecoder: Decoder[ActivitySessionDataId] =
    Decoder.forLong.map(ActivitySessionDataId.apply)

  implicit val jsonEncoder: Encoder[ActivitySessionDataId] =
    Encoder.forLong.contramap(_.id)

  given Show[ActivitySessionDataId] = Show.show(_.id.toString)
