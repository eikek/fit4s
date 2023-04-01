package fit4s.activities.data

final class ActivityId(val id: Long) extends AnyVal {
  override def toString = s"ActivityId($id)"
}

object ActivityId {
  def apply(id: Long): ActivityId = new ActivityId(id)

  implicit val ordering: Ordering[ActivityId] =
    Ordering.by(_.id)
}
