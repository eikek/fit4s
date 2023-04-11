package fit4s.activities

import fit4s.activities.records.RGeoPlace
import fit4s.data.Position

trait GeoLookup[F[_]] {

  def lookup(position: Position): F[Option[RGeoPlace]]
}
