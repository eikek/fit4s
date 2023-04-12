package fit4s.cli.activity

import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.data.{ActivityDetailResult, ActivityId}
import fit4s.cli.{CliConfig, FormatDefinition, SharedOpts, Styles}

import java.time.ZoneId

object ShowCmd extends SharedOpts with FormatDefinition {

  final case class Options(id: ActivityId)

  val opts: Opts[Options] =
    Opts
      .argument[ActivityId]("activity-id")
      .map(Options)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        result <- log.activityDetails(opts.id)
        _ <- result match {
          case Some(r) => showResults(r)(cliCfg.timezone)
          case None    => IO.println(s"No activity found.")
        }
      } yield ExitCode.Error
    }

  def showResults(r: ActivityDetailResult)(implicit zoneId: ZoneId): IO[Unit] = {
    val header = show"${r.activity.timestamp}, ${r.activity.name}"

    IO.println(
      List(
        header.in(Styles.headerOne),
        List.fill(header.length)("-").mkString.in(Styles.headerOne),
        r.tags
          .map(_.name)
          .mkString("[", ", ", "]")
          .in(Styles.tags) + "  " + r.activity.device.show.in(Styles.device),

        //
        "",

        //
        (r.location.location / r.activity.path).show.in(Styles.device),
        r.stravaId
          .map(id => s"https://strava.com/activities/${id.id}")
          .show
          .in(Styles.device)
      ).mkString("\n")
    )
  }
}
