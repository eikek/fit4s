package fit4s.geocode

import cats.effect._
import cats.syntax.all._
import fit4s.data.Position

private[geocode] trait PlaceCache[F[_]] {

  def cached(f: Position => F[Option[Place]]): Position => F[Option[Place]]

}

private[geocode] object PlaceCache {

  def apply[F[_]: Async](size: Int): F[PlaceCache[F]] =
    Ref.of[F, Map[Position, Option[Place]]](Map.empty).map {
      state => (f: Position => F[Option[Place]]) => pos =>
        state.get.map(_.get(pos)).flatMap {
          case Some(r) => r.pure[F]
          case None =>
            f(pos).flatTap { r =>
              state.update { s =>
                if (s.size >= size) s.drop(1).updated(pos, r)
                else s.updated(pos, r)
              }
            }
        }
    }
}
