package fit4s.geocode

import io.circe.Decoder

final class NominatimPlaceId(val id: Long) extends AnyVal {
  override def toString = s"PlaceId($id)"
}

object NominatimPlaceId {
  def apply(id: Long): NominatimPlaceId = new NominatimPlaceId(id)

  implicit val jsonDecoder: Decoder[NominatimPlaceId] =
    Decoder.decodeLong.map(apply)
}
