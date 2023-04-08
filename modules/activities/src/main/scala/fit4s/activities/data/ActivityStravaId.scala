package fit4s.activities.data

final class ActivityStravaId(val id: Long) extends AnyVal {
  override def toString = s"ActivityStravaId($id)"
}

object ActivityStravaId {
  def apply(id: Long): ActivityStravaId = new ActivityStravaId(id)
}
