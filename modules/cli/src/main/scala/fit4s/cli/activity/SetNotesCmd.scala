package fit4s.cli.activity

import cats.effect._
import cats.syntax.all._

import fit4s.activities.data.ActivityId
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object SetNotesCmd extends SharedOpts {

  final case class Options(id: ActivityId, notes: Option[String])

  val opts: Opts[Options] = {
    val reset =
      Opts
        .flag("remove", "Removes any notes")
        .map(_ => Option.empty[String])

    val name =
      Opts.option[String]("notes", "Set these notes").orNone

    val id = Opts.option[ActivityId]("id", "The activity id to change")

    (id, reset.orElse(name)).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        _ <- log.setActivityNotes(opts.id, opts.notes)
        _ <- IO.println("Notes changed.")
      } yield ExitCode.Success
    }
}
