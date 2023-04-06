package fit4s.cli.activity

import cats.effect.{Clock, ExitCode, IO}
import cats.syntax.all._
import fit4s.activities.ActivityLog
import fit4s.activities.data.ActivitySessionSummary
import fit4s.cli.FormatDefinition._
import fit4s.cli.{ActivitySelection, CliConfig, CliError, Styles}
import fit4s.profile.types.Sport

import java.time.ZoneId

object SummaryCmd {

  final case class Options(
      query: ActivitySelection
  )

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    ActivityLog[IO](cliCfg.jdbcConfig, cliCfg.timezone).use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        zone = cliCfg.timezone

        query <- ActivitySelection
          .makeCondition(opts.query, zone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        summary <- log.activitySummary(query)
        out = summary.map(makeSummaryTable(2, zone))

        _ <- out.traverse_ { case (head, data) =>
          IO.println(head) *> IO.println("\n") *> IO.println(data) *> IO.println("\n")
        }
      } yield ExitCode.Success
    }

  def makeHeader(header: String): String = {
    val len = header.length
    val dashes = List.fill(len)('-').mkString
    s"$header\n$dashes".in(Styles.headerOne)
  }

  def makeSummaryTable(indent: Int, zoneId: ZoneId)(
      s: ActivitySessionSummary
  ): (String, String) = {
    val sp = List.fill(indent)(' ').mkString
    implicit val sport: Sport = s.sport
    implicit val zone: ZoneId = zoneId

    val pairs = List(
      Some("Activities" -> s.count.toString),
      Some("Date" -> show"${s.startTime.asDate} -- ${s.endTime.asDate}"),
      Some("Distance" -> s.distance.show),
      Some("Time" -> s.movingTime.show),
      s.totalAscend.map(a => "Elevation" -> s"${a.meter.toInt}m"),
      Some("Calories" -> s.calories.show),
      Some("Temp." -> show"${s.minTemp} to ${s.maxTemp}")
        .filter(_ => s.minTemp.isDefined || s.maxTemp.isDefined),
      s.avgHr.map(hr => "Heart rate" -> hr.show),
      Some(
        "Speed avg" -> show"${s.avgSpeed}"
      ).filter(_ =>
        s.maxSpeed.exists(_.meterPerSecond > 0) || s.avgSpeed.exists(_.meterPerSecond > 0)
      )
    )

    val colLen = pairs.flatMap(_.map(_._1.length)).max + 2
    val lines = pairs
      .collect { case Some((name, value)) =>
        val s = List.fill(colLen - name.length)(" ").mkString
        s"$sp${name.in(Styles.summaryFieldName)}:$s${value.in(Styles.summaryFieldValue)}"
      }
      .mkString("\n")

    makeHeader(s.sport.toString) -> lines

  }
}
