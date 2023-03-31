package fit4s.activities.data

final class ActivityId(val id: Long) extends AnyVal
object ActivityId {
  def apply(id: Long): ActivityId = new ActivityId(id)
}
