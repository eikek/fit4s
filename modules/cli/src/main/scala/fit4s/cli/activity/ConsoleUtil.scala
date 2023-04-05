package fit4s.cli.activity

import fit4s.activities.data.ActivitySessionSummary

import java.time.{Duration, ZoneId}

object ConsoleUtil {
  val bold = Console.BOLD
  val reset = Console.RESET
  val red = Console.RED
  val green = Console.GREEN
  val boldRed = s"$bold$red"

  def makeHeader(header: String): String = {
    val len = header.length
    val dashes = List.fill(len)('-').mkString
    s"$bold$green$header\n$dashes$reset"
  }

  def makeSummaryTable(indent: Int, zoneId: ZoneId)(
      s: ActivitySessionSummary
  ): (String, String) = {
    val sp = List.fill(indent)(' ').mkString

    makeHeader(s.sport.toString) ->
      s"""${sp}Activities: $bold${s.count}$reset
         |${sp}Date:       ${s.startTime.atZone(zoneId).toLocalDate} -- ${s.endTime
          .atZone(zoneId)
          .toLocalDate}
         |${sp}Distance:   $bold${s.distance}$reset
         |${sp}Time:       $bold${dur(s.movingTime)}$reset
         |${sp}Elevation:  $bold${s.totalAscend.map(_.meter.toInt).getOrElse("-")}m$reset
         |${sp}Calories:   $bold${s.calories}$reset
         |${sp}Temp. °C:   ${s.minTemp.getOrElse("-")} to ${s.maxTemp.getOrElse("-")}
         |${sp}Heart rate:  ${s.avgHr.getOrElse("-")}
         |""".stripMargin
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

}
