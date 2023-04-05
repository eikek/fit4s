package fit4s.cli.activity

import fit4s.activities.data.ActivitySessionSummary
import fit4s.data.Speed
import fit4s.profile.types.Sport

import java.time.{Duration, Instant, ZoneId}

object ConsoleUtil {
  val bold = Console.BOLD
  val reset = Console.RESET
  val red = Console.RED
  val green = Console.GREEN
  val boldRed = s"$bold$red"

  val cFname = ""
  val cFvalue = bold

  def makeHeader(header: String): String = {
    val len = header.length
    val dashes = List.fill(len)('-').mkString
    s"$bold$green$header\n$dashes$reset"
  }

  def makeSummaryTable(indent: Int, zoneId: ZoneId)(
      s: ActivitySessionSummary
  ): (String, String) = {
    val sp = List.fill(indent)(' ').mkString
    def ld(i: Instant) = i.atZone(zoneId).toLocalDate

    val pairs = List(
      Some("Activities" -> s.count.toString),
      Some("Date" -> s"${ld(s.startTime)} -- ${ld(s.endTime)}"),
      Some("Distance" -> s.distance.toString),
      Some("Time" -> dur(s.movingTime)),
      s.totalAscend.map(a => "Elevation" -> s"${a.meter.toInt}m"),
      Some("Calories" -> s.calories.toString),
      Some("Temp." -> s"${s.minTemp.getOrElse("-")} to ${s.maxTemp.getOrElse("-")}")
        .filter(_ => s.minTemp.isDefined || s.maxTemp.isDefined),
      s.avgHr.map(hr => "Heart rate" -> hr.toString),
      Some(
        "Speed avg" -> s"${speedStr(s.avgSpeed, s.sport)}"
      ).filter(_ =>
        s.maxSpeed.exists(_.meterPerSecond > 0) || s.avgSpeed.exists(_.meterPerSecond > 0)
      )
    )

    val colLen = pairs.flatMap(_.map(_._1.length)).max + 2
    val lines = pairs
      .collect { case Some((name, value)) =>
        val s = List.fill(colLen - name.length)(" ").mkString
        s"$sp$cFname$name$reset:$s$cFvalue$value$reset"
      }
      .mkString("\n")

    makeHeader(s.sport.toString) -> lines

  }

  private def speedStr(speed: Option[Speed], sport: Sport): String =
    (speed, sport) match {
      case (Some(sp), Sport.Swimming) => s"${minTomss(sp.minPer100m)} min/100m"
      case (Some(sp), Sport.Running)  => s"${minTomss(sp.minPer1k)} min/km"
      case (Some(sp), Sport.Walking)  => s"${minTomss(sp.minPer1k)} min/km"
      case (Some(sp), Sport.Hiking)   => s"${minTomss(sp.minPer1k)} min/km"
      case (Some(sp), _)              => sp.toString
      case _                          => ""
    }

  private def dur(d: Duration): String = {
    val secs = d.toSeconds
    List(
      secs / 3600 -> "h",
      (secs - ((secs / 3600) * 3600)) / 60 -> "min"
    ).filter(_._1 > 0)
      .map { case (v, u) => s"$v$u" }
      .mkString(" ")
  }

  private def minTomss(min: Double): String = {
    val minutes = min.floor.toInt
    val secs = (min - minutes) * 60
    f"$minutes:${secs.toInt}%02d"
  }
}
