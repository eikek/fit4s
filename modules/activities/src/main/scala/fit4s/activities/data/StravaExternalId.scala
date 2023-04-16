package fit4s.activities.data

import io.circe.Decoder
import org.http4s.Uri

final class StravaExternalId(val id: Long) extends AnyVal {
  override def toString = s"StravaExternal($id)"
}

object StravaExternalId {
  def apply(id: Long): StravaExternalId = new StravaExternalId(id)

  implicit val stravaIdDecoder: Decoder[StravaExternalId] =
    Decoder.decodeLong.map(StravaExternalId.apply)

  implicit val pathSegmentEncoder: Uri.Path.SegmentEncoder[StravaExternalId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
}
