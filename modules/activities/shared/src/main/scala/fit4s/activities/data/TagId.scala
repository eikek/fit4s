package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

final class TagId(val id: Long) extends AnyVal with Ordered[TagId] {
  override def toString: String = s"TagId($id)"

  override def compare(that: TagId): Int = id.compare(that.id)
}

object TagId {
  def apply(id: Long): TagId = new TagId(id)

  implicit val ordering: Ordering[TagId] =
    Ordering.by(_.id)

  implicit val tagIdDecoder: Decoder[TagId] =
    Decoder.forLong.map(TagId.apply)

  implicit val tagIdEncoder: Encoder[TagId] =
    Encoder.forLong.contramap(_.id)

  given Show[TagId] = Show.show(_.id.toString)
}
