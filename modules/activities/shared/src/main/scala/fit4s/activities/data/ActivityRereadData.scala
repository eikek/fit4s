package fit4s.activities.data

import fs2.io.file.Path

import fit4s.data.FileId

import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs

final case class ActivityRereadData(
    id: ActivityId,
    fileId: FileId,
    path: String,
    location: String
):

  val file: Path =
    Path(location) / Path(path)

object ActivityRereadData:
  given Encoder[FileId] =
    Encoder.forString.contramap(_.asString)
  given Encoder[ActivityRereadData] = MapBasedCodecs.deriveEncoder
