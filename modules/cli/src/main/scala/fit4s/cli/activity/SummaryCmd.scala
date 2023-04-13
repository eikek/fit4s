package fit4s.cli.activity

import cats.Show
import cats.effect.{Clock, ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.ActivityQuery
import fit4s.activities.data.{ActivitySessionSummary, Page}
import fit4s.cli.FormatDefinition._
import fit4s.cli._
import fit4s.data._
import fit4s.profile.types.Sport
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import io.circe.{Encoder, Json}

import java.time.{Duration, ZoneId}

object SummaryCmd extends SharedOpts {

  final case class Options(
      query: ActivitySelection,
      page: Page,
      format: OutputFormat
  )

  val opts: Opts[Options] =
    (activitySelectionOps, pageOpts, outputFormatOpts).mapN(Options)

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        zone = cliCfg.timezone

        query <- ActivitySelection
          .makeCondition(opts.query, zone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        summary <- log.activitySummary(ActivityQuery(query, opts.page))
        _ <-
          opts.format.fold(
            printJson(summary)(zone),
            printText(summary, zone)
          )
      } yield ExitCode.Success
    }

  def printJson(summary: Vector[ActivitySessionSummary])(implicit
      zoneId: ZoneId
  ): IO[Unit] =
    IO.println(
      summary
        .map(Summary.from)
        .map { s =>
          implicit val sport: Sport = s.sport
          s.asJson
        }
        .asJson
        .spaces2
    )

  def printText(summary: Vector[ActivitySessionSummary], zone: ZoneId) =
    summary
      .map(makeSummaryTable(zone))
      .traverse_ { case (head, data) =>
        IO.println(head) *> IO.println("\n") *> IO.println(data) *> IO.println("\n")
      }

  def makeHeader(header: String): String = {
    val len = header.length
    val dashes = List.fill(len)('-').mkString
    s"$header\n$dashes".in(Styles.headerOne)
  }

  def makeSummaryTable(zoneId: ZoneId)(s: ActivitySessionSummary): (String, String) = {
    implicit val sport: Sport = s.sport
    implicit val zone: ZoneId = zoneId

    val repr = Summary.from(s)
    makeHeader(s.sport.toString) -> repr.show
  }

  final case class StartEnd[A](start: A, end: A)
  object StartEnd {
    implicit def show[A](implicit sa: Show[A]): Show[StartEnd[A]] =
      Show.show(ra => show"${ra.start} -- ${ra.end}")

    implicit def jsonEncoder[A: Encoder]: Encoder[StartEnd[A]] =
      Encoder.instance(r => Json.obj("start" -> r.start.asJson, "end" -> r.end.asJson))
  }
  final case class Summary(
      sport: Sport,
      count: Int,
      dateRange: StartEnd[DateInstant],
      distance: Distance,
      time: Duration,
      elevation: Option[Distance],
      calories: Calories,
      temperature: StartEnd[Option[Temperature]],
      avgCadence: Option[Cadence],
      avgHr: Option[HeartRate],
      avgSpeed: Option[Speed],
      avgGrade: Option[Percent],
      intensityFactor: Option[IntensityFactor],
      trainingStressScore: Option[TrainingStressScore]
  )

  object Summary {
    def from(a: ActivitySessionSummary): Summary =
      Summary(
        a.sport,
        a.count,
        StartEnd(a.startTime.asDate, a.endTime.asDate),
        a.distance,
        a.movingTime,
        a.totalAscend,
        a.calories,
        StartEnd(a.minTemp, a.maxTemp),
        a.avgCadence,
        a.avgHr,
        a.avgSpeed,
        a.avgGrade,
        a.avgIntensity,
        a.avgTss
      )

    implicit def jsonEncoder(implicit zoneId: ZoneId, sport: Sport): Encoder[Summary] = {
      import DataJsonEncoder._

      // some bug raises an error for unused zone, but it is used in the following macro
      @annotation.unused
      val (_, _) = (zoneId, sport)

      deriveEncoder
    }

    implicit def show(implicit zoneId: ZoneId, sport: Sport): Show[Summary] =
      Show.show { s =>
        val pairs = List(
          Some("Activities" -> s.count.toString),
          Some("Date" -> s.dateRange.show),
          Some("Distance" -> s.distance.show),
          Some("Time" -> s.time.show),
          s.elevation.map(a => "Elevation" -> s"${a.meter.toInt}m"),
          Some("Calories" -> s.calories.show),
          Some("Temp." -> s.temperature.show).filter(_ =>
            s.temperature.start.isDefined || s.temperature.end.isDefined
          ),
          s.avgHr.map(hr => "Heart rate" -> hr.show),
          s.avgSpeed
            .map(spd => "Speed avg" -> spd.show)
            .filter(_ => s.avgSpeed.exists(_.meterPerSecond > 0)),
          s.avgGrade.map(p => "Grade" -> p.show),
          s.intensityFactor.map(iff => "IF avg" -> iff.show),
          s.trainingStressScore.map(tss => "TSS avg" -> tss.show)
        )

        val colLen = pairs.flatMap(_.map(_._1.length)).max + 2
        pairs
          .collect { case Some((name, value)) =>
            val s = List.fill(colLen - name.length)(" ").mkString
            s"  ${name.in(Styles.summaryFieldName)}:$s${value.in(Styles.summaryFieldValue)}"
          }
          .mkString("\n")
      }
  }
}
