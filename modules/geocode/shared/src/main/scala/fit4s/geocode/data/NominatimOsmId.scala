package fit4s.geocode.data

import io.bullet.borer.{Decoder, Encoder}

final class NominatimOsmId(val id: Long) extends AnyVal:
  override def toString = s"OsmId($id)"

object NominatimOsmId:
  def apply(id: Long): NominatimOsmId = new NominatimOsmId(id)

  given Decoder[NominatimOsmId] = Decoder.forLong.map(apply)
  given Encoder[NominatimOsmId] = Encoder.forLong.contramap(_.id)
