package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class LocationId(val id: Long) extends AnyVal {
  override def toString: String = s"LocationId($id)"
}

object LocationId {
  def apply(id: Long): LocationId = new LocationId(id)

  implicit val ordering: Ordering[LocationId] =
    Ordering.by(_.id)

  implicit val jsonDecoder: Decoder[LocationId] =
    Decoder.forLong.map(LocationId.apply)

  implicit val jsonEncoder: Encoder[LocationId] =
    Encoder.forLong.contramap(_.id)

  given Show[LocationId] = Show.show(_.id.toString)
}
