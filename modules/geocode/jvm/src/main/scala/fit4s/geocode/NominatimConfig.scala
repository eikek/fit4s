package fit4s.geocode

import com.github.eikek.borer.compats.http4s.Http4sCodec
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import org.http4s.Uri
import org.http4s.implicits.*

case class NominatimConfig(
    baseUrl: Uri,
    maxReqPerSecond: Float,
    cacheSize: Int
)

object NominatimConfig extends Http4sCodec:
  object Defaults:
    val baseUrl = uri"https://nominatim.openstreetmap.org/reverse"
    val maxReqPerSecond = 1f
    val cacheSize = 100
  val default =
    NominatimConfig(Defaults.baseUrl, Defaults.maxReqPerSecond, Defaults.cacheSize)

  given Encoder[NominatimConfig] = deriveEncoder
