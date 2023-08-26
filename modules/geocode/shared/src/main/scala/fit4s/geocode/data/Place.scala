package fit4s.geocode.data

import fit4s.data.{Position, Semicircle}

case class Place(
    place_id: NominatimPlaceId,
    osm_type: String,
    osm_id: NominatimOsmId,
    lat: Semicircle,
    lon: Semicircle,
    display_name: String,
    address: Address,
    boundingbox: BoundingBox
) {

  val position: Position = Position(lat, lon)
}
