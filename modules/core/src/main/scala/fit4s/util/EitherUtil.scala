package fit4s.util

private[fit4s] trait EitherUtil:

  implicit final class EitherCtorSyntax[A](self: A):
    def asLeft[B]: Either[A, B] = Left(self)
    def asRight[B]: Either[B, A] = Right(self)

  implicit final class VectorEitherMap[A](self: Vector[A]):
    def mapEither[B, C](f: A => Either[B, C]): Either[B, Vector[C]] =
      if (self.isEmpty) Right(Vector.empty)
      else
        self.foldLeft(Vector.empty[C].asRight[B]) { (res, el) =>
          res.flatMap(v => f(el).map(c => v.appended(c)))
        }

private[fit4s] object EitherUtil extends EitherUtil
