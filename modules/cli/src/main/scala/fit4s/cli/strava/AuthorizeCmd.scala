package fit4s.cli.strava

import cats.effect._
import com.monovore.decline.Opts
import fit4s.cli.{CliConfig, SharedOpts}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object AuthorizeCmd extends SharedOpts {

  case class Options(timeout: FiniteDuration)

  val opts: Opts[Options] = {
    val timeout = Opts
      .option[Int](
        "timeout",
        "Timeout in seconds to wait for the authorization process to complete"
      )
      .withDefault(60)
      .map(FiniteDuration(_, TimeUnit.SECONDS))

    timeout.map(Options)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      IO(cliConfig.stravaAuthConfig).flatMap {
        case Some(oauthCfg) =>
          log.strava
            .initOAuth(oauthCfg, opts.timeout)
            .flatTap {
              case Some(token) =>
                IO.println(s"Got token for scopes: ${token.scope.asString}")
              case None => IO.println("No token received!")
            }
            .as(ExitCode.Success)
        case None =>
          IO.println(s"No strava client_id and client_secret configured!")
            .as(ExitCode.Error)
      }
    }
}