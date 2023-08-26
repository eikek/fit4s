package fit4s.strava.data

import io.bullet.borer._

final class StravaScope(raw: String) {
  val scopes = raw.split(',').map(_.trim.toLowerCase).filter(_.nonEmpty).toSet

  def hasActivityRead: Boolean = scopes.contains("activity:read")

  def hasActivityWrite: Boolean = scopes.contains("activity:write")

  def asString = scopes.mkString(",")

  override def toString = s"Scopes($asString)"
}
object StravaScope {
  val activityReadAndWrite =
    StravaScope("activity:read,activity:read_all,activity:write,profile:read_all")

  def apply(scopes: String): StravaScope = new StravaScope(scopes)

  implicit val jsonDecoder: Decoder[StravaScope] =
    Decoder.forString.map(apply)

  implicit val jsonEncoder: Encoder[StravaScope] =
    Encoder.forString.contramap(_.asString)
}
