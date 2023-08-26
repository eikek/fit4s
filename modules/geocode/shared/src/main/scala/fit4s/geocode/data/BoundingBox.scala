package fit4s.geocode.data

import fit4s.common.borer.DataJsonCodec.*
import fit4s.data.Semicircle

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class BoundingBox(
    lat1: Semicircle,
    lat2: Semicircle,
    lng1: Semicircle,
    lng2: Semicircle
)

object BoundingBox {
  implicit val jsonDecoder: Decoder[BoundingBox] = deriveDecoder
  implicit val jsonEncoder: Encoder[BoundingBox] = deriveEncoder
}
