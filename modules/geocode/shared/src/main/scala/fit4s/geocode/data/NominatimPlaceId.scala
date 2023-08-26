package fit4s.geocode.data

import io.bullet.borer.{Decoder, Encoder}

final class NominatimPlaceId(val id: Long) extends AnyVal {
  override def toString = s"PlaceId($id)"
}

object NominatimPlaceId {
  def apply(id: Long): NominatimPlaceId = new NominatimPlaceId(id)

  implicit val jsonDecoder: Decoder[NominatimPlaceId] =
    Decoder.forLong.map(apply)

  implicit val jsonEncoder: Encoder[NominatimPlaceId] =
    Encoder.forLong.contramap(_.id)
}
