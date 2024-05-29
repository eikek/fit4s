package fit4s.cli.tag

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.data.TagName
import fit4s.cli.*

import com.monovore.decline.Opts

object UnlinkCmd extends SharedOpts {

  final case class Options(query: ActivitySelection, tags: NonEmptyList[TagName])

  val opts: Opts[Options] = {
    val addTags =
      Opts.options[TagName]("tag", help = "Remove these tags from selected activities.")

    (activitySelectionOps, addTags).mapN(Options.apply)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        query <- ActivitySelection
          .makeCondition(opts.query, cliCfg.timezone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        n <- log.tagRepository.unlinkTags(query, opts.tags)
        _ <- IO.println(s"$n tags unlinked.")
      } yield ExitCode.Success
    }
}
