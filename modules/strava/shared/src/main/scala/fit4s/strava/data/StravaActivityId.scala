package fit4s.strava.data

import io.bullet.borer._
import io.bullet.borer.derivation.MapBasedCodecs._
import org.http4s.Uri

final class StravaActivityId(val id: Long) extends AnyVal:
  override def toString = s"StravaActivity($id)"

object StravaActivityId:
  def apply(id: Long): StravaActivityId = new StravaActivityId(id)

  implicit val stravaIdDecoder: Decoder[StravaActivityId] =
    Decoder.forLong.map(StravaActivityId.apply)

  implicit val stravaIdEncoder: Encoder[StravaActivityId] =
    Encoder.forLong.contramap(_.id)

  implicit val pathSegmentEncoder: Uri.Path.SegmentEncoder[StravaActivityId] =
    Uri.Path.SegmentEncoder.longSegmentEncoder.contramap(_.id)
