package fit4s.activities.data

final class TagId(val id: Long) extends AnyVal

object TagId {
  def apply(id: Long): TagId = new TagId(id)

  implicit val ordering: Ordering[TagId] =
    Ordering.by(_.id)
}