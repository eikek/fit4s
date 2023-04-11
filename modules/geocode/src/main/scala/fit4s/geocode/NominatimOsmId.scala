package fit4s.geocode

import io.circe.Decoder

final class NominatimOsmId(val id: Long) extends AnyVal {
  override def toString = s"OsmId($id)"
}

object NominatimOsmId {
  def apply(id: Long): NominatimOsmId = new NominatimOsmId(id)

  implicit val jsonDecoder: Decoder[NominatimOsmId] =
    Decoder.decodeLong.map(apply)
}
