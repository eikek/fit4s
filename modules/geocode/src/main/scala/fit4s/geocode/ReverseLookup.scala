package fit4s.geocode

import cats.Applicative
import cats.effect.{Async, Resource}
import fit4s.data.Position

trait ReverseLookup[F[_]] {

  def lookup(position: Position): F[Option[Place]]

  /** Version without potential caching. */
  def lookupRaw(position: Position): F[Option[Place]]

}

object ReverseLookup {
  private def empty[F[_]: Applicative]: ReverseLookup[F] =
    new ReverseLookup[F] {
      override def lookup(position: Position) = Applicative[F].pure(None)
      override def lookupRaw(position: Position) = Applicative[F].pure(None)
    }

  def osm[F[_]: Async](cfg: NominatimConfig): Resource[F, ReverseLookup[F]] =
    if (cfg.maxReqPerSecond <= 0) Resource.pure(empty[F])
    else NominatimOSM.resource(cfg)
}
