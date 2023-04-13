package fit4s.cli.activity

import cats.Show
import cats.effect.{ExitCode, IO}
import cats.syntax.all._
import com.monovore.decline.Opts
import fit4s.activities.data.{ActivityDetailResult, ActivityId}
import fit4s.activities.records.{RActivityLap, RActivitySession}
import fit4s.cli._
import fit4s.data.Distance
import fit4s.profile.types.Sport

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}
import scala.math.Ordering.Implicits.infixOrderingOps

object ShowCmd extends SharedOpts with FormatDefinition {

  final case class Options(id: ActivityId, format: OutputFormat)

  val opts: Opts[Options] = {
    val id = Opts
      .argument[ActivityId]("activity-id")

    (id, outputFormatOpts).mapN(Options)
  }

  def apply(cliCfg: CliConfig, opts: Options): IO[ExitCode] =
    activityLog(cliCfg).use { log =>
      for {
        result <- log.activityDetails(opts.id)
        _ <- result match {
          case Some(r) =>
            opts.format match {
              case OutputFormat.Text => showResults(r)(cliCfg.timezone)
              case OutputFormat.Json =>
                IO.println(ActivityListResultJson.encodeDetail(r).spaces2)
            }

          case None =>
            IO.println(s"No activity found.")
        }
      } yield ExitCode.Error
    }

  def showResults(r: ActivityDetailResult)(implicit zoneId: ZoneId): IO[Unit] = {
    val header = show"${r.activity.name}"
    val stravaLink =
      r.stravaId
        .map(id => s"https://strava.com/activities/${id.id}".in(Styles.lightGrey))
        .getOrElse("")

    IO.println(
      List(
        header.in(Styles.headerOne),
        List.fill(header.length)("-").mkString.in(Styles.headerOne),
        r.sessions.head.startTime.show.in(Styles.activityName),
        stravaLink,
        tags(r),
        notes(r),
        " ",
        //
        r.sessions.toList
          .map(showSession(r, _))
          .mkString("\n"),
        " ",
        device(r),
        fileId(r),
        filePath(r)
      ).filter(_.nonEmpty).mkString("\n")
    )

  }

  def fileId(r: ActivityDetailResult) =
    s"File-Id: ${r.activity.activityFileId.asString}".in(Styles.device)

  def showSession(r: ActivityDetailResult, s: RActivitySession)(implicit
      zoneId: ZoneId
  ): String = {
    implicit val sport: Sport = s.sport

    List(
      (s.sport -> s.subSport).show.in(Styles.sport ++ Styles.bold),
      distanceLocationAndTime(r, s),
      " ",
      avgMaxTable(s),
      " ",
      propertiesTable(s),
      " ",
      showLaps(r.laps.getOrElse(s.id, Nil))
    ).filter(_.nonEmpty).mkString("\n")
  }

  private def showLaps(laps: List[RActivityLap])(implicit zoneId: ZoneId): String =
    if (laps.isEmpty) ""
    else {
      val labels =
        List(
          "Time",
          "Moved",
          "Distance",
          "Avg Speed",
          "Avg Cad",
          "Avg Hr",
          "Avg Power",
          "Stroke",
          "Stroke Count",
          "Stroke Dst"
        )
      val dataCols = laps.map { l =>
        implicit val sport: Sport = l.sport
        List(
          l.startTime.asTime.show,
          l.movingTime.show,
          l.distance.show,
          l.avgSpeed.show,
          l.avgCadence.show,
          l.avgHr.show,
          l.avgPower.show,
          l.swimStroke.show,
          l.strokeCount.show,
          l.avgStrokeDistance.show
        )
      }

      val allRows = labels :: dataCols

      def maxColLen(index: Int) =
        allRows.map(_(index).length).max + 2

      def isColEmpty(index: Int): Boolean =
        dataCols.forall(_(index).isEmpty)

      def spaces(count: Int) = List.fill(count)(" ").mkString

      allRows.zipWithIndex
        .map { case (cols, rowIdx) =>
          cols.zipWithIndex
            .filterNot(t => isColEmpty(t._2))
            .map { case (value, idx) =>
              val v = if (rowIdx == 0) value.in(Styles.lightGrey) else value
              s"$v${spaces(maxColLen(idx) - value.length)}"
            }
            .mkString("  ")
        }
        .mkString("\n")
    }

  private def distanceLocationAndTime(r: ActivityDetailResult, s: RActivitySession) = {
    val fromTo =
      (r.startPlace.get(s.id), r.startEndDistance.get(s.id))
        .mapN { (start, dst) =>
          if (dst < Distance.meter(800)) s" Round trip from ${start.location}"
          else
            r.endPlace.get(s.id) match {
              case Some(end) if end.location == start.location =>
                val sStreet = start.road.map(s => s"/$s").getOrElse("")
                val eStreet = end.road.map(s => s"/$s").getOrElse("")
                s" from ${start.location}$sStreet to ${end.location}$eStreet"
              case Some(end) =>
                s" from ${start.location} to ${end.location}"
              case None =>
                s" from ${start.location}"
            }
        }
        .getOrElse("")

    show"${s.distance.show.in(Styles.bold)}$fromTo in ${s.movingTime.show.in(Styles.bold)}"
  }

  def notes(r: ActivityDetailResult) =
    r.activity.notes.map(_.trim) match {
      case Some(n) =>
        n.split("\r?\n")
          .map(line => s"  $line")
          .mkString("\n")
      case None => ""
    }

  def tags(r: ActivityDetailResult) =
    if (r.tags.isEmpty) ""
    else {
      val str = r.tags.map(_.name.name.in(Styles.tags)).mkString(" * ")
      s"\n  $str"
    }

  def avgMaxTable(s: RActivitySession) = {
    implicit val sport: Sport = s.sport
    val data = List(
      ("", "Avg", "Max"),
      ("Speed", s.avgSpeed.show, s.maxSpeed.show),
      ("Cadence", s.avgCadence.show, s.maxCadence.show),
      ("Power", s.normPower.show, s.maxPower.show),
      ("Heart rate", s.avgHr.show, s.maxHr.show),
      ("Temp.", s.avgTemp.show, s.maxTemp.show),
      ("Stroke Count", s.avgStrokeCount.show, ""),
      ("Stroke Distance", s.avgStrokeDistance.show, "")
    ).filter(t => t._2.nonEmpty || t._3.nonEmpty)
    val colLen1 = data.map(_._1.length).max + 2
    val colLen2 = data.map(_._2.length).max + 2

    data.zipWithIndex
      .map { case ((label, avg, max), index) =>
        val sp1 = List.fill(colLen1 - label.length)(" ").mkString
        val sp2 = List.fill(colLen2 - avg.length)(" ").mkString
        val a =
          if (index == 0) avg.in(Styles.bold)
          else if (label == "Temp.") avg.in(Styles.temperature(s.avgTemp))
          else avg.in(Styles.fieldValue)
        val m =
          if (index == 0) max.in(Styles.bold)
          else if (label == "Temp.") max.in(Styles.temperature(s.maxTemp))
          else max.in(Styles.fieldValue)
        s"${label.in(Styles.lightGrey)}$sp1$a$sp2$m"
      }
      .mkString("\n")
  }

  def propertiesTable(s: RActivitySession) = {
    val data = List(
      ("Elapsed", s.elapsedTime.show),
      ("Elevation", s.totalAscend.show),
      ("Avg Grade", s.avgGrade.show),
      ("min Temp.", s.minTemp.show.in(Styles.temperature(s.minTemp))),
      ("Calories", s.calories.show),
      ("TSS", s.tss.show),
      ("IF", s.iff.show),
      ("Swim stroke", s.swimStroke.show),
      ("Pool length", s.poolLength.show),
      ("# pool length", s.numPoolLength.show)
    ).filter(_._2.nonEmpty)
    val colLen1 = data.map(_._1.length).max + 2
    data
      .map { case (label, value) =>
        val sp1 = List.fill(colLen1 - label.length)(" ").mkString
        s"${label.in(Styles.lightGrey)}$sp1$value"
      }
      .mkString("\n")
  }

  def filePath(r: ActivityDetailResult) = {
    val path = r.location.location / r.activity.path
    path.show.in(Styles.device)
  }

  def device(r: ActivityDetailResult) =
    show"Device: ${r.activity.device}".in(Styles.device)

  implicit override def instantShow(implicit zoneId: ZoneId): Show[Instant] =
    Show.show { i =>
      val zoned = i.atZone(zoneId)
      zoned.truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.RFC_1123_DATE_TIME)
    }
}
