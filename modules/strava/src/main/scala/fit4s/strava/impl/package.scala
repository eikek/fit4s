package fit4s.strava

import fit4s.strava.data.StravaAccessToken
import org.http4s.{AuthScheme, Credentials, Request}
import org.http4s.headers.Authorization

package object impl {

  implicit final class RequestOps[F[_]](val self: Request[F]) {
    def putAuth(accessToken: StravaAccessToken) =
      self.putHeaders(
        Authorization(Credentials.Token(AuthScheme.Bearer, accessToken.token))
      )
  }
}
