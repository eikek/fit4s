package fit4s.geocode.data

import io.bullet.borer.{Decoder, Encoder}

final class NominatimOsmId(val id: Long) extends AnyVal:
  override def toString = s"OsmId($id)"

object NominatimOsmId:
  def apply(id: Long): NominatimOsmId = new NominatimOsmId(id)

  implicit val jsonDecoder: Decoder[NominatimOsmId] =
    Decoder.forLong.map(apply)

  implicit val jsonEncoder: Encoder[NominatimOsmId] =
    Encoder.forLong.contramap(_.id)
