package fit4s.activities.data

final class ActivityDataId(val id: Long) extends AnyVal
object ActivityDataId {
  def apply(id: Long): ActivityDataId = new ActivityDataId(id)
}
