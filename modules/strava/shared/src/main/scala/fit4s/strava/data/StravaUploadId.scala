package fit4s.strava.data

import io.bullet.borer.*
import org.http4s.Uri

final class StravaUploadId(val id: Long) extends AnyVal:
  override def toString = s"StravaUploadId($id)"

object StravaUploadId:
  def apply(id: Long): StravaUploadId = new StravaUploadId(id)

  implicit val jsonDecoder: Decoder[StravaUploadId] =
    Decoder.forLong.map(StravaUploadId.apply)

  implicit val jsonEncoder: Encoder[StravaUploadId] =
    Encoder.forLong.contramap(_.id)

  implicit val pathSegmentEncoder: Uri.Path.SegmentEncoder[StravaUploadId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
