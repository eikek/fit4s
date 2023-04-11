package fit4s.geocode

import fit4s.data.Semicircle

final case class BoundingBox(
    lat1: Semicircle,
    lat2: Semicircle,
    lng1: Semicircle,
    lng2: Semicircle
)
