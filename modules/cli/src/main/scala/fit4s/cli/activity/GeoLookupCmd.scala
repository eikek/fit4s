package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*

import fit4s.activities.data.ActivityId
import fit4s.cli.*

import com.monovore.decline.Opts

object GeoLookupCmd extends SharedOpts {

  case class Options(ids: List[ActivityId])

  val opts: Opts[Options] = {
    val ids = Opts
      .options[ActivityId](
        "id",
        "The activity ids to do geo lookups for. If not given all that are missing the data are used."
      )
      .orEmpty
    ids.map(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        _ <- log.geoLookup(opts.ids, id => IO.println(s"Looking up ${id.id}..."))
      } yield ExitCode.Success
    }
}
