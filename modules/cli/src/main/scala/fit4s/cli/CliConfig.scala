package fit4s.cli

import java.time.ZoneId

import scala.concurrent.duration.*

import cats.MonadThrow
import cats.effect.Async
import cats.syntax.all.*
import fs2.io.file.Files

import fit4s.activities.JdbcConfig
import fit4s.geocode.NominatimConfig
import fit4s.strava.{StravaAppCredentials, StravaClientConfig}

import io.bullet.borer.*
import io.bullet.borer.NullOptions.given
import io.bullet.borer.derivation.MapBasedCodecs.*

case class CliConfig(
    timezone: ZoneId,
    jdbcConfig: JdbcConfig,
    nominatimConfig: NominatimConfig,
    stravaConfig: StravaClientConfig,
    httpTimeout: Duration,
    stravaAuthConfig: Option[StravaAppCredentials]
)

object CliConfig {

  private def cfg[F[_]: Files: MonadThrow] =
    (
      ConfigValues.timeZone,
      ConfigValues.jdbc[F],
      ConfigValues.nominatim,
      ConfigValues.strava,
      ConfigValues.httpTimeout,
      ConfigValues.stravaOAuth
    ).mapN(CliConfig.apply)

  def load[F[_]: Async: Files]: F[CliConfig] =
    cfg[F].load[F]

  given Encoder[Duration] = Encoder.forString.contramap(_.toString)

  given Encoder[ZoneId] = Encoder.forString.contramap(_.getId())
  given Encoder[CliConfig] = deriveEncoder
}
