package fit4s.activities.data

final class ActivityGeoPlaceId(val id: Long) extends AnyVal {
  override def toString = s"ActivityGeoPlaceId($id)"
}

object ActivityGeoPlaceId {
  def apply(id: Long): ActivityGeoPlaceId = new ActivityGeoPlaceId(id)
}
