package fit4s.strava.data

import io.circe.Decoder
import org.http4s.Uri

final class StravaActivityId(val id: Long) extends AnyVal {
  override def toString = s"StravaActivity($id)"
}

object StravaActivityId {
  def apply(id: Long): StravaActivityId = new StravaActivityId(id)

  implicit val stravaIdDecoder: Decoder[StravaActivityId] =
    Decoder.decodeLong.map(StravaActivityId.apply)

  implicit val pathSegmentEncoder: Uri.Path.SegmentEncoder[StravaActivityId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
}
