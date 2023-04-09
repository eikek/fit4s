package fit4s.geocode

import org.http4s.Uri
import org.http4s.implicits._

case class NominatimConfig(
    baseUrl: Uri = NominatimConfig.defaultUri,
    maxReqPerSecond: Float = 1f
)

object NominatimConfig {
  val defaultUri = uri"https://nominatim.openstreetmap.org/reverse"
}
