package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import com.monovore.decline.Opts
import fit4s.activities.data.ActivityId
import fit4s.cli.{CliConfig, SharedOpts}

object ShowCmd extends SharedOpts {

  final case class Options(id: ActivityId)

  val opts: Opts[Options] =
    Opts
      .argument[ActivityId]("activity-id")
      .map(Options)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        _ <- IO.println(s"${opts.id} $log Not implemented :(")
      } yield ExitCode.Error
    }
}
