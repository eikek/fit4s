package fit4s.activities.data

import cats.Show

import io.bullet.borer.*

opaque type GeoPlaceId = Long
object GeoPlaceId:
  def apply(id: Long): GeoPlaceId = id

  given Ordering[GeoPlaceId] = Ordering.Long
  given Show[GeoPlaceId] = Show.show(_.id.toString)
  given Encoder[GeoPlaceId] = Encoder.forLong.contramap(_.id)
  given Decoder[GeoPlaceId] = Decoder.forLong.map(apply)

extension (id: GeoPlaceId) def id: Long = id
