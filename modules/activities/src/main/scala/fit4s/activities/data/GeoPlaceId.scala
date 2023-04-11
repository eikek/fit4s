package fit4s.activities.data

import cats.Eq

final class GeoPlaceId(val id: Long) extends AnyVal {
  override def toString = s"GeoPlaceId($id)"
}

object GeoPlaceId {
  def apply(id: Long): GeoPlaceId = new GeoPlaceId(id)

  implicit val ordering: Ordering[GeoPlaceId] =
    Ordering.by(_.id)

  implicit val eq: Eq[GeoPlaceId] =
    Eq.by(_.id)
}
