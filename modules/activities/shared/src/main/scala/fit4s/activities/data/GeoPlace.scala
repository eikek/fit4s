package fit4s.activities.data

import fit4s.common.borer.DataJsonCodec.*
import fit4s.data.Position
import fit4s.geocode.data.*

import io.bullet.borer.*
import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs

final case class GeoPlace(
    id: GeoPlaceId,
    osmPlaceId: NominatimPlaceId,
    osmId: NominatimOsmId,
    position: Position,
    road: Option[String],
    location: String,
    country: Option[String],
    countryCode: CountryCode,
    postCode: Option[PostCode],
    boundingBox: BoundingBox
)

object GeoPlace:
  given Encoder[GeoPlace] = MapBasedCodecs.deriveEncoder
  given Decoder[GeoPlace] = MapBasedCodecs.deriveDecoder
