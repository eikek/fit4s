package fit4s.cli.tag

import cats.effect.{ExitCode, IO}
import fit4s.activities.ActivityLog
import fit4s.activities.data.{Page, TagName}
import fit4s.cli.CliConfig

object ListCmd {

  final case class Options(nameFilter: Option[TagName])

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    ActivityLog[IO](cliCfg.jdbcConfig, cliCfg.timezone).use { log =>
      for {
        _ <- log.tagRepository
          .listTags(opts.nameFilter, Page.unlimited)
          .map(_.name.name)
          .evalTap(IO.println)
          .compile
          .drain

      } yield ExitCode.Success
    }
}
