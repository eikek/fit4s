package fit4s.activities

import org.http4s.Uri
import org.http4s.implicits._

case class StravaConfig(
    authUrl: Uri,
    tokenUri: Uri,
    apiUrl: Uri,
    gearCacheSize: Int
)

object StravaConfig {
  object Defaults {
    val authUrl = uri"https://www.strava.com/oauth/authorize"
    val apiUrl = uri"https://www.strava.com/api/v3"
    val tokenUrl = apiUrl / "oauth" / "token"
    val gearCacheSize = 50
  }

  val default =
    StravaConfig(
      Defaults.authUrl,
      Defaults.tokenUrl,
      Defaults.apiUrl,
      Defaults.gearCacheSize
    )
}
