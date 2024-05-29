package fit4s.cli.location

import cats.effect.*
import cats.syntax.all.*
import fs2.Stream

import fit4s.activities.data.Page
import fit4s.cli.RecordJsonEncoder.*
import fit4s.cli.{CliConfig, OutputFormat, SharedOpts}

import com.monovore.decline.Opts
import io.bullet.borer.Json

object ListCmd extends SharedOpts {

  final case class Options(contains: Option[String], page: Page, format: OutputFormat)

  val opts: Opts[Options] = {
    val contains =
      Opts.option[String]("contains", "Look for locations containing this string").orNone

    (contains, pageOpts, outputFormatOpts).mapN(Options.apply)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      (for {
        location <- log.locationRepository.listLocations(opts.contains, opts.page)
        _ <- Stream.eval {
          opts.format.fold(
            IO.println(Json.encode(location).toUtf8String),
            IO.println(
              f"${location._1.id.id}%4d ${location._1.location} ${location._2}"
            )
          )
        }
      } yield ()).compile.drain.as(ExitCode.Success)
    }

}
