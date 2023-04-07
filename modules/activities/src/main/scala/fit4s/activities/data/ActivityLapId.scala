package fit4s.activities.data

import cats.Eq

final class ActivityLapId(val id: Long) extends AnyVal {
  override def toString = s"ActivityLap($id)"
}

object ActivityLapId {
  def apply(id: Long): ActivityLapId = new ActivityLapId(id)

  implicit val ordering: Ordering[ActivityLapId] =
    Ordering.by(_.id)

  implicit val eq: Eq[ActivityLapId] =
    Eq.by(_.id)
}
