package fit4s.strava

import fit4s.http.borer.Http4sCodec

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*
import org.http4s.Uri
import org.http4s.implicits._

final case class StravaClientConfig(
    authUrl: Uri,
    tokenUri: Uri,
    apiUrl: Uri,
    gearCacheSize: Int
)

object StravaClientConfig extends Http4sCodec:
  object Defaults:
    val authUrl = uri"https://www.strava.com/oauth/authorize"
    val apiUrl = uri"https://www.strava.com/api/v3"
    val tokenUrl = apiUrl / "oauth" / "token"
    val gearCacheSize = 50

  val default =
    StravaClientConfig(
      Defaults.authUrl,
      Defaults.tokenUrl,
      Defaults.apiUrl,
      Defaults.gearCacheSize
    )

  given Encoder[StravaClientConfig] = deriveEncoder
