package fit4s.activities.data

import cats.Eq

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs._

final case class Tag(id: TagId, name: TagName)

object Tag:
  implicit val jsonDecoder: Decoder[Tag] = deriveDecoder
  implicit val jsonEncoder: Encoder[Tag] = deriveEncoder

  def create(name: TagName): Tag = Tag(TagId(-1L), name)

  given Eq[Tag] = Eq.by(_.id.id)
