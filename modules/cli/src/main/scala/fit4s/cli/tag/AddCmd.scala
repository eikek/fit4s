package fit4s.cli.tag

import cats.data.NonEmptyList
import cats.effect.{Clock, ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.ActivityLog
import fit4s.activities.data.TagName
import fit4s.cli.{ActivitySelection, SharedOpts, CliConfig, CliError}

object AddCmd extends SharedOpts {

  final case class Options(query: ActivitySelection, tags: NonEmptyList[TagName])

  val opts: Opts[Options] = {
    val addTags =
      Opts.options[TagName]("tag", help = "Add these tags to selected activities.")

    (activitySelectionOps, addTags).mapN(AddCmd.Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    ActivityLog[IO](cliCfg.jdbcConfig, cliCfg.timezone).use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        query <- ActivitySelection
          .makeCondition(opts.query, cliCfg.timezone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        _ <- log.tagRepository.linkTags(query, opts.tags)
        _ <- IO.println("Done.")
      } yield ExitCode.Success
    }
}
