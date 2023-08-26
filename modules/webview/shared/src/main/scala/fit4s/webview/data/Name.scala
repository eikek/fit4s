package fit4s.webview.data

import io.bullet.borer.{Decoder, Encoder}
import io.bullet.borer.derivation.MapBasedCodecs.*

case class Name(name: String)
object Name:
  given Decoder[Name] = deriveDecoder
  given Encoder[Name] = deriveEncoder
