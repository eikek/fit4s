package fit4s.common.borer

import io.bullet.borer.*

trait DecoderSyntax:
  extension [A](delegate: Decoder[A])
    def emap[B](f: A => Either[String, B]): Decoder[B] =
      delegate.mapWithReader((r, a) => f(a).fold(r.validationFailure, identity))
