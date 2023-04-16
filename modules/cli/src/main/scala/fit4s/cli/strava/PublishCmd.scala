package fit4s.cli.strava

import cats.effect._
import com.monovore.decline.Opts
import fit4s.cli.{ActivitySelection, CliConfig, SharedOpts}

import java.time.Instant

object PublishCmd extends SharedOpts {

  final case class Options(
      query: Option[ActivitySelection]
  )

  val opts: Opts[Options] =
    activitySelectionOps.orNone.map(Options)

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      for {
        _ <- IO.println(opts)
        after = Instant.parse("2023-04-14T10:00:00Z")
        before = Instant.now()
        res <- log.strava.listActivities(
          cliConfig.stravaAuthConfig.get,
          after,
          before,
          1,
          30
        )
        _ <- IO.println(res)
      } yield ExitCode.Success
    }
}
