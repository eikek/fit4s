package fit4s.activities.data

final class ActivitySessionId(val id: Long) extends AnyVal {
  override def toString = s"ActivitySessionId($id)"
}

object ActivitySessionId {
  def apply(id: Long): ActivitySessionId = new ActivitySessionId(id)

  implicit val ordering: Ordering[ActivitySessionId] =
    Ordering.by(_.id)
}
