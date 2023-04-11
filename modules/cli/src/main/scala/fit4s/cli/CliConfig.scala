package fit4s.cli

import cats.Monad
import cats.effect.Async
import cats.syntax.all._
import ciris._
import fit4s.activities.JdbcConfig
import fit4s.geocode.NominatimConfig
import fs2.io.file.{Files, Path}
import org.http4s.Uri

import java.time.ZoneId

case class CliConfig(
    timezone: ZoneId,
    jdbcConfig: JdbcConfig,
    nominatimConfig: NominatimConfig
)

object CliConfig {

  private def cfg[F[_]: Files: Monad] =
    (Env.zone, Env.jdbc[F], Env.nominatimCfg).mapN(CliConfig.apply)

  def load[F[_]: Async]: F[CliConfig] =
    cfg[F].load[F]

  private object Env {
    private val envPrefix = "FIT4S_ACTIVITIES"
    private val propPrefix = "fit4s.activities"

    implicit val zoneDecoder: ConfigDecoder[String, ZoneId] =
      ConfigDecoder[String].mapOption("TimeZone") { s =>
        if (ZoneId.getAvailableZoneIds.contains(s)) ZoneId.of(s).some
        else None
      }

    def nominatimCfg = {
      val defcfg = NominatimConfig()
      val baseUrl =
        env(s"${envPrefix}_NOMINATIM_URL")
          .or(prop(s"$propPrefix.nominatim.url"))
          .map(Uri.unsafeFromString)
          .default(defcfg.baseUrl)

      val maxReq =
        env(s"${envPrefix}_NOMINATIM_MAX_REQPS")
          .or(prop(s"$propPrefix.nominatim.max_reqps"))
          .map(_.toFloat)
          .default(defcfg.maxReqPerSecond)

      (baseUrl, maxReq).mapN(NominatimConfig.apply)
    }

    def defaultStorageDir[F[_]: Files: Monad]: F[Path] =
      for {
        home <- Files[F].userHome
        dir = home / ".config" / "fit4s-activities"
        _ <- Files[F].createDirectories(dir)
      } yield dir

    def jdbcDefault[F[_]: Files: Monad] =
      ConfigValue.eval[F, JdbcConfig](
        defaultStorageDir[F].map(JdbcConfig.fromDirectory).map(ConfigValue.default)
      )

    val jdbcFromDir =
      env(s"${envPrefix}_STORAGE_DIR")
        .or(prop(s"$propPrefix.storage.dir"))
        .map(Path.apply)
        .map(JdbcConfig.fromDirectory)

    val jdbcFromConnection =
      (
        env(s"${envPrefix}_DB_URL").or(prop(s"$propPrefix.db.url")),
        env(s"${envPrefix}_DB_USER").or(prop(s"$propPrefix.db.user")),
        env(s"${envPrefix}_DB_PASSWORD").or(prop(s"$propPrefix.db.password")).redacted
      ).mapN(JdbcConfig.apply)

    def jdbc[F[_]: Files: Monad] =
      jdbcFromConnection.or(jdbcFromDir).or(jdbcDefault[F])

    val zone =
      env(s"${envPrefix}_TIMEZONE")
        .or(prop(s"$propPrefix.timezone"))
        .default("Europe/Berlin")
        .as[ZoneId]
  }
}
