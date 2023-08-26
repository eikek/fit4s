package fit4s.webview.data

import io.bullet.borer.{Decoder, Encoder}
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class Notes(notes: String)
object Notes:
  given Decoder[Notes] = deriveDecoder
  given Encoder[Notes] = deriveEncoder
