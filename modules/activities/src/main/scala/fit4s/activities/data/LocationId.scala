package fit4s.activities.data

final class LocationId(val id: Long) extends AnyVal {
  override def toString: String = s"LocationId($id)"
}

object LocationId {
  def apply(id: Long): LocationId = new LocationId(id)

  implicit val ordering: Ordering[LocationId] =
    Ordering.by(_.id)
}
