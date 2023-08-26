package fit4s.webview.data

import fit4s.activities.data.TagName

import io.bullet.borer.Decoder
import io.bullet.borer.derivation.MapBasedCodecs.*

case class TagRename(from: TagName, to: TagName)

object TagRename:
  given Decoder[TagRename] = deriveDecoder
