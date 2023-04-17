package fit4s.cli

import java.time.ZoneId

import cats.Monad
import cats.effect.Async
import cats.syntax.all._
import fs2.io.file.Files

import fit4s.activities.JdbcConfig
import fit4s.geocode.NominatimConfig
import fit4s.strava.{StravaAppCredentials, StravaClientConfig}

case class CliConfig(
    timezone: ZoneId,
    jdbcConfig: JdbcConfig,
    nominatimConfig: NominatimConfig,
    stravaConfig: StravaClientConfig,
    stravaAuthConfig: Option[StravaAppCredentials]
)

object CliConfig {

  private def cfg[F[_]: Files: Monad] =
    (
      ConfigValues.timeZone,
      ConfigValues.jdbc[F],
      ConfigValues.nominatim,
      ConfigValues.strava,
      ConfigValues.stravaOAuth
    ).mapN(CliConfig.apply)

  def load[F[_]: Async]: F[CliConfig] =
    cfg[F].load[F]
}
