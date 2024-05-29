package fit4s.geocode

import cats.Applicative
import cats.effect.Async

import fit4s.data.Position
import fit4s.geocode.data.Place

import org.http4s.client.Client

trait ReverseLookup[F[_]]:

  def lookup(position: Position): F[Option[Place]]

  /** Version without potential caching. */
  def lookupRaw(position: Position): F[Option[Place]]

object ReverseLookup:
  private def empty[F[_]: Applicative]: ReverseLookup[F] =
    new ReverseLookup[F]:
      override def lookup(position: Position) = Applicative[F].pure(None)
      override def lookupRaw(position: Position) = Applicative[F].pure(None)

  def osm[F[_]: Async](client: Client[F], cfg: NominatimConfig): F[ReverseLookup[F]] =
    if (cfg.maxReqPerSecond <= 0) Async[F].pure(empty[F])
    else NominatimOSM(client, cfg)
