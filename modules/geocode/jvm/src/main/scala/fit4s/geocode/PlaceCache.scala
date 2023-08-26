package fit4s.geocode

import cats.effect.*

import fit4s.cats.util.Cache
import fit4s.data.Position
import fit4s.geocode.data.Place

private[geocode] object PlaceCache {
  def memory[F[_]: Sync](size: Int): F[Cache[F, Position, Place]] =
    Cache.memory[F, Position, Place](size)
}
