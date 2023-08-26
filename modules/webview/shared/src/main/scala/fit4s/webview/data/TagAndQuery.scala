package fit4s.webview.data

import cats.data.NonEmptyList

import fit4s.activities.data.TagName
import fit4s.webview.json.BasicJsonCodec._

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class TagAndQuery(tags: NonEmptyList[TagName], query: Option[String])

object TagAndQuery {

  implicit val jsonCodec: Codec[TagAndQuery] =
    deriveCodec
}
