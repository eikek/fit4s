package fit4s.cli.location

import cats.effect._
import cats.syntax.all._
import io.circe.syntax._
import com.monovore.decline.Opts
import fs2.Stream
import fit4s.activities.data.Page
import fit4s.cli.{CliConfig, OutputFormat, SharedOpts}
import fit4s.cli.RecordJsonEncoder._

object ListCmd extends SharedOpts {

  final case class Options(contains: Option[String], page: Page, format: OutputFormat)

  val opts: Opts[Options] = {
    val contains =
      Opts.option[String]("contains", "Look for locations containing this string").orNone

    (contains, pageOpts, outputFormatOpts).mapN(Options)
  }

  def apply(cliConfig: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliConfig).use { log =>
      (for {
        location <- log.locationRepository.listLocations(opts.contains, opts.page)
        _ <- Stream.eval {
          opts.format.fold(
            IO.println(location.asJson.spaces2),
            IO.println(
              f"${location._1.id.id}%4d ${location._1.location} ${location._2}"
            )
          )
        }
      } yield ()).compile.drain.as(ExitCode.Success)
    }

}
