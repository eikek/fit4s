package fit4s.cli.strava

import cats.effect._
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.ActivityQuery
import fit4s.activities.data.Page
import fit4s.cli.{ActivitySelection, CliConfig, SharedOpts}

object UnlinkCmd extends SharedOpts {

  case class Options(selection: ActivitySelection, page: Page)

  val opts: Opts[Options] =
    (activitySelectionOps, pageOpts).mapN(Options)

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      for {
        query <- resolveQuery(opts.selection, cliConfig.timezone)
        aq = ActivityQuery(query, opts.page)

        n <- log.strava.unlink(aq)
        _ <- IO.println(s"Removed strava ids for $n activities")
      } yield ExitCode.Success
    }
}