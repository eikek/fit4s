package fit4s.activities.data

import io.circe.Decoder
import org.http4s.Uri

final class StravaUploadId(val id: Long) extends AnyVal {
  override def toString = s"StravaUploadId($id)"
}

object StravaUploadId {
  def apply(id: Long): StravaUploadId = new StravaUploadId(id)

  implicit val stravaUploadIdDecoder: Decoder[StravaUploadId] =
    Decoder.decodeLong.map(StravaUploadId.apply)

  implicit val pathSegmentEncoder: Uri.Path.SegmentEncoder[StravaUploadId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
}
