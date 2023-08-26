package fit4s.activities.data

import io.bullet.borer.{Decoder, Encoder}

opaque type CountryCode = String
object CountryCode:
  def apply(cc: String): CountryCode = cc.toLowerCase

  given Encoder[CountryCode] = Encoder.forString.contramap(_.code)
  given Decoder[CountryCode] = Decoder.forString.map(apply)

extension (cc: CountryCode) def code: String = cc
