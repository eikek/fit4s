package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class ActivityGeoPlaceId(val id: Long) extends AnyVal:
  override def toString = s"ActivityGeoPlaceId($id)"

object ActivityGeoPlaceId:
  def apply(id: Long): ActivityGeoPlaceId = new ActivityGeoPlaceId(id)

  implicit val jsonEncoder: Encoder[ActivityGeoPlaceId] =
    Encoder.forLong.contramap(_.id)

  implicit val jsonDecoder: Decoder[ActivityGeoPlaceId] =
    Decoder.forLong.map(apply)

  given Show[ActivityGeoPlaceId] = Show.show(_.id.toString)
