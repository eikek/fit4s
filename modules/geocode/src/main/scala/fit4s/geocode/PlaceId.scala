package fit4s.geocode

import io.circe.Decoder

final class PlaceId(val id: Long) extends AnyVal {
  override def toString = s"PlaceId($id)"
}

object PlaceId {
  def apply(id: Long): PlaceId = new PlaceId(id)

  implicit val jsonDecoder: Decoder[PlaceId] =
    Decoder.decodeLong.map(apply)
}
