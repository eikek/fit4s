package fit4s.geocode

import cats.effect.{Async, Resource}
import fit4s.data.Position

trait ReverseLookup[F[_]] {

  def lookup(position: Position): F[Option[Place]]

  /** Version without potential caching. */
  def lookupRaw(position: Position): F[Option[Place]]

}

object ReverseLookup {
  def osm[F[_]: Async](cfg: NominatimConfig): Resource[F, ReverseLookup[F]] =
    NominatimOSM.resource(cfg)
}
