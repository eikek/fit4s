package fit4s.cli

import cats.Monad
import cats.effect.Async
import cats.syntax.all._
import fit4s.activities.{JdbcConfig, StravaOAuthConfig}
import fit4s.geocode.NominatimConfig
import fs2.io.file.Files

import java.time.ZoneId

case class CliConfig(
    timezone: ZoneId,
    jdbcConfig: JdbcConfig,
    nominatimConfig: NominatimConfig,
    stravaOAuthConfig: Option[StravaOAuthConfig]
)

object CliConfig {

  private def cfg[F[_]: Files: Monad] =
    (
      ConfigValues.timeZone,
      ConfigValues.jdbc[F],
      ConfigValues.nominatim,
      ConfigValues.stravaOAuth
    ).mapN(CliConfig.apply)

  def load[F[_]: Async]: F[CliConfig] =
    cfg[F].load[F]
}
