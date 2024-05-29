package fit4s.cli.activity

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO}
import cats.syntax.all.*

import fit4s.activities.data.ActivityId
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object DeleteCmd extends SharedOpts {

  case class Options(ids: NonEmptyList[ActivityId], hard: Boolean)

  val opts: Opts[Options] = {
    val ids = Opts.options[ActivityId]("id", "The activity ids to delete")
    val hard =
      Opts.flag("hard", "Really remove from database instead of soft-deleting").orFalse
    (ids, hard).mapN(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        n <- log.deleteActivities(opts.ids, opts.hard)
        _ <- IO.println(s"Removed $n activities.")
      } yield ExitCode.Success
    }
}
