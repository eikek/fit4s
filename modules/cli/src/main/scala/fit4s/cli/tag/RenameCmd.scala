package fit4s.cli.tag

import cats.effect.{ExitCode, IO}
import cats.syntax.all._

import fit4s.activities.data.TagName
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object RenameCmd extends SharedOpts {

  case class Options(from: TagName, to: TagName)

  val opts: Opts[Options] = {
    val f = Opts.argument[TagName]("from")
    val t = Opts.argument[TagName]("to")
    (f, t).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        result <- log.tagRepository.rename(opts.from, opts.to)
        rc <-
          if (result) IO.println("Done.").as(ExitCode.Success)
          else IO.println("Tag not found").as(ExitCode.Error)
      } yield rc
    }
}
