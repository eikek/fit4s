package fit4s.cli

import cats.Monad
import cats.syntax.all._
import ciris._
import fit4s.activities.{JdbcConfig, StravaAuthConfig, StravaConfig}
import fit4s.geocode.NominatimConfig
import fs2.io.file.{Files, Path}
import org.http4s.Uri

import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

object ConfigValues {
  private[this] val envPrefix = "FIT4S"
  private[this] val values = new AtomicReference[Map[String, Option[String]]](Map.empty)

  def getAll: Map[String, Option[String]] = values.get()

  private lazy val userHome: Path =
    sys.props
      .get("user.home")
      .orElse(sys.env.get("HOME"))
      .map(Path.apply)
      .getOrElse(sys.error(s"No user home directory available!"))

  val nominatim = {
    val cfg = NominatimConfig.default
    val baseUrl =
      config("NOMINATIM_BASE_URL", cfg.baseUrl.renderString).as[Uri]

    val maxReq =
      config("NOMINATIM_MAX_REQPS", cfg.maxReqPerSecond.toString)
        .as[Float]

    val cacheSize =
      config("NOMINATIM_PLACE_CACHE_SIZE", cfg.cacheSize.toString).as[Int]

    (baseUrl, maxReq, cacheSize).mapN(NominatimConfig.apply)
  }

  def jdbc[F[_]: Files: Monad] = {
    val jdbcFromDir =
      config(
        "DB_STORAGE_DIR",
        (userHome / ".config" / "fit4s-activities").absolute.toString
      )
        .map(Path.apply)
        .evalMap(p => Files[F].createDirectories(p).as(p))
        .map(JdbcConfig.fromDirectory)

    val jdbcFromConnection =
      (
        config("DB_URL"),
        config("DB_USER", "sa"),
        config("DB_PASSWORD", "sa").redacted
      ).mapN(JdbcConfig.apply)

    jdbcFromConnection.or(jdbcFromDir)
  }

  val timeZone =
    config("TIMEZONE", "Europe/Berlin").as[ZoneId]

  val strava = {
    val authUrl =
      config("STRAVA_AUTH_URL", StravaConfig.Defaults.authUrl.renderString)
        .as[Uri]

    val tokenUrl =
      config("STRAVA_TOKEN_URL", StravaConfig.Defaults.tokenUrl.renderString)
        .as[Uri]

    val apiUrl =
      config("STRAVA_API_URL", StravaConfig.Defaults.apiUrl.renderString).as[Uri]

    val gearCacheSize =
      config("STRAVA_GEAR_CACHE_SIZE", StravaConfig.Defaults.gearCacheSize.toString)
        .as[Int]

    (authUrl, tokenUrl, apiUrl, gearCacheSize).mapN(StravaConfig.apply)
  }

  val stravaOAuth = {
    val clientId = config("STRAVA_CLIENT_ID")
    val clientSecret = config("STRAVA_CLIENT_SECRET")

    (clientId, clientSecret).mapN(StravaAuthConfig.apply)
  }.option

  implicit private def zoneDecoder: ConfigDecoder[String, ZoneId] =
    ConfigDecoder[String].mapOption("TimeZone") { s =>
      if (ZoneId.getAvailableZoneIds.contains(s)) ZoneId.of(s).some
      else None
    }

  implicit private def uriDecoder: ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("uri") { s =>
      Uri.fromString(s).toOption
    }

  private[this] def addName(name: String, defaultValue: Option[String]) =
    values.updateAndGet(m => m.updated(name, defaultValue))

  private[this] def config(
      name: String,
      default: Option[String]
  ): ConfigValue[Effect, String] = {
    val fullName = s"${envPrefix}_${name.toUpperCase}"
    addName(fullName, default)
    val propName = fullName.toLowerCase.replace('_', '.')
    val cv = prop(propName).or(env(fullName))
    default.map(cv.default(_)).getOrElse(cv)
  }

  private[this] def config(name: String): ConfigValue[Effect, String] = config(name, None)

  private[this] def config(name: String, defval: String): ConfigValue[Effect, String] =
    config(name, Some(defval))
}
