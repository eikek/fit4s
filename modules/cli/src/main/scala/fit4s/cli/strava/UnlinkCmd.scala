package fit4s.cli.strava

import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.data.{ActivityQuery, Page}
import fit4s.cli.{ActivitySelection, CliConfig, SharedOpts}

import com.monovore.decline.Opts

object UnlinkCmd extends SharedOpts {

  case class Options(selection: ActivitySelection, page: Page)

  val opts: Opts[Options] =
    (activitySelectionOps, pageOpts).mapN(Options.apply)

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
