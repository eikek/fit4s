package fit4s.activities.impl

import cats.effect._
import cats.syntax.all._
import cats.Functor
import cats.effect.std.Random
import org.http4s.multipart.{Boundary, Multipart, Multiparts, Part}
import scodec.bits.ByteVector

object MyMultiparts {
  def forSync[F[_]](implicit F: Sync[F]): F[Multiparts[F]] =
    Random.scalaUtilRandom[F].map(fromRandom[F])

  def fromRandom[F[_]](random: Random[F])(implicit F: Functor[F]): Multiparts[F] =
    new Multiparts[F] {
      def boundary: F[Boundary] =
        random
          .nextBytes(12)
          .map(ByteVector.view)
          .map(_.toBase58)
          .map(s => s"------------------------$s")
          .map(Boundary.apply)

      def multipart(parts: Vector[Part[F]]): F[Multipart[F]] =
        F.map(boundary)(Multipart(parts, _))
    }
}
