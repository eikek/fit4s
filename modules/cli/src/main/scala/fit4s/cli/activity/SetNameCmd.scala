package fit4s.cli.activity

import cats.effect._
import cats.syntax.all._

import fit4s.activities.data.ActivityId
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object SetNameCmd extends SharedOpts {

  final case class Options(id: ActivityId, name: Option[String])

  val opts: Opts[Options] = {
    val reset =
      Opts
        .flag("reset", "Use a generated activity name")
        .map(_ => Option.empty[String])

    val name =
      Opts.option[String]("name", "Set this name").orNone

    val id = Opts.option[ActivityId]("id", "The activity id to change")

    (id, reset.orElse(name)).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        _ <- log.setActivityName(opts.id, opts.name)
        _ <- IO.println("Name changed.")
      } yield ExitCode.Success
    }
}
