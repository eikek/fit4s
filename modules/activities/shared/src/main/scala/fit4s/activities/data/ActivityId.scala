package fit4s.activities.data

import cats.Eq
import cats.Show

import io.bullet.borer.*
import org.http4s.Uri

final class ActivityId(val id: Long) extends AnyVal:
  override def toString = s"ActivityId($id)"

object ActivityId:
  def apply(id: Long): ActivityId = new ActivityId(id)

  implicit val ordering: Ordering[ActivityId] =
    Ordering.by(_.id)

  implicit val eq: Eq[ActivityId] =
    Eq.by(_.id)

  implicit val jsonDecoder: Decoder[ActivityId] =
    Decoder.forLong.map(ActivityId.apply)

  implicit val jsonEncoder: Encoder[ActivityId] =
    Encoder.forLong.contramap(_.id)

  given Show[ActivityId] = Show.show(_.id.toString)

  given Uri.Path.SegmentEncoder[ActivityId] =
    Uri.Path.SegmentEncoder.instance(id => Uri.Path.Segment(id.id.toString))
