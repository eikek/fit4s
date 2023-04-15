package fit4s.activities

import org.http4s.Uri
import org.http4s.implicits._

case class StravaOAuthConfig(
    clientId: String,
    clientSecret: String,
    authUrl: Uri,
    tokenUri: Uri,
    apiUrl: Uri
)

object StravaOAuthConfig {
  object Defaults {
    val authUrl = uri"https://www.strava.com/oauth/authorize"
    val apiUrl = uri"https://www.strava.com/api/v3"
    val tokenUrl = apiUrl / "oauth" / "token"
  }

  def default(clientId: String, clientSecret: String) =
    StravaOAuthConfig(
      clientId,
      clientSecret,
      Defaults.authUrl,
      Defaults.tokenUrl,
      Defaults.apiUrl
    )
}
