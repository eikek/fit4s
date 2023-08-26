package fit4s.cli.tag

import cats.effect.{ExitCode, IO}

import fit4s.activities.data.TagName
import fit4s.cli.{CliConfig, SharedOpts}

import com.monovore.decline.Opts

object RemoveCmd extends SharedOpts {

  case class Options(tag: TagName)

  val opts: Opts[Options] =
    Opts.argument[TagName]("tag").map(Options.apply)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        result <- log.tagRepository.remove(opts.tag)
        rc <-
          if (result > 0) IO.println(s"Removed $result tag(s)").as(ExitCode.Success)
          else IO.println("Tag not found").as(ExitCode.Error)
      } yield rc
    }
}
