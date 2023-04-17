package fit4s.strava.data

import io.circe.Decoder

final class StravaAthleteId(val id: Long) extends AnyVal {
  override def toString = s"StravaAthleteId($id)"
}

object StravaAthleteId {
  def apply(id: Long): StravaAthleteId = new StravaAthleteId(id)

  implicit val jsonDecoder: Decoder[StravaAthleteId] =
    Decoder.decodeLong.map(apply)
}
