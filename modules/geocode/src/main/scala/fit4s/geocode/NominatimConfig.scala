package fit4s.geocode

import org.http4s.Uri
import org.http4s.implicits._

case class NominatimConfig(
    baseUrl: Uri,
    maxReqPerSecond: Float
)

object NominatimConfig {
  object Defaults {
    val baseUrl = uri"https://nominatim.openstreetmap.org/reverse"
    val maxReqPerSecond = 1f
  }
  val default = NominatimConfig(Defaults.baseUrl, Defaults.maxReqPerSecond)
}
