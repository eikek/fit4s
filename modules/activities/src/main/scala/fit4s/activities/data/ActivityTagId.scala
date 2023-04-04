package fit4s.activities.data

final class ActivityTagId(val id: Long) extends AnyVal

object ActivityTagId {
  def apply(id: Long): ActivityTagId = new ActivityTagId(id)

  implicit val ordering: Ordering[ActivityTagId] =
    Ordering.by(_.id)
}