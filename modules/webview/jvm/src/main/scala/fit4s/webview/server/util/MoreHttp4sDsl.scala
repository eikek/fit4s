package fit4s.webview.server.util

import cats.data.ValidatedNel
import cats.syntax.all.*
import cats.{Applicative, Monad}

import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec.given
import fit4s.webview.data.RequestFailure

import org.http4s.*
import org.http4s.dsl.Http4sDsl

trait MoreHttp4sDsl[F[_]] { self: Http4sDsl[F] =>

  implicit final class ValidatedToResponse(
      resp: ValidatedNel[ParseFailure, F[Response[F]]]
  )(implicit F: Monad[F]):
    def orBadRequest =
      resp.fold(errs => BadRequest(RequestFailure(errs)), identity)

  implicit final class OptionToResponse[A](resp: Option[A])(implicit
      e: EntityEncoder[F, A],
      F: Applicative[F]
  ):
    def orNotFound(msg: String) =
      resp.fold(NotFoundF(msg))(Ok(_))

  def InternalServerError(ex: Throwable): Response[F] =
    ErrorResponse(ex)

  def NotFound(msg: String): Response[F] =
    ErrorResponse(Status.NotFound, msg)

  def NotFoundF(msg: String)(implicit F: Applicative[F]): F[Response[F]] =
    NotFound(msg).pure[F]
}
