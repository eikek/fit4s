package fit4s.activities.data

import io.bullet.borer.{Decoder, Encoder}

final class PostCode(val zip: String) extends AnyVal {
  override def toString = s"PostCode($zip)"
}

object PostCode {
  def apply(zip: String): PostCode = new PostCode(zip)

  implicit val encoder: Encoder[PostCode] = Encoder.forString.contramap(_.zip)
  implicit val decoder: Decoder[PostCode] = Decoder.forString.map(apply)
}
