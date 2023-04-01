package fit4s.activities.data

final class ActivitySessionDataId(val id: Long) extends AnyVal

object ActivitySessionDataId {
  def apply(id: Long): ActivitySessionDataId = new ActivitySessionDataId(id)

  implicit val ordering: Ordering[ActivitySessionDataId] =
    Ordering.by(_.id)
}
