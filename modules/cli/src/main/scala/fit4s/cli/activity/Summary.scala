package fit4s.cli.activity

import cats.syntax.all._
import fit4s.activities.data.CatsInstances._
import fit4s.activities.records.ActivitySessionRecord
import fit4s.data.{Calories, Distance, HeartRate, Temperature}

import java.time.Duration

case class Summary(
    count: Int,
    distance: Distance,
    time: Duration,
    elevation: Option[Distance],
    avgHr: Option[HeartRate],
    calories: Calories,
    maxTemp: Option[Temperature],
    minTemp: Option[Temperature]
) {

  private val bold = ConsoleUtil.bold
  private val reset = ConsoleUtil.reset

  def summaryTable(indent: Int): String = {
    val sp = List.fill(indent)(' ').mkString
    s"""${sp}Activities: $bold$count$reset
       |${sp}Distance:   $bold$distance$reset
       |${sp}Time:       $bold$time$reset
       |${sp}Elevation:  $bold${elevation.map(_.meter).getOrElse("-")}m$reset
       |${sp}Calories:   $bold$calories$reset
       |${sp}Temp. °C:   ${minTemp.getOrElse("-")} to ${maxTemp.getOrElse("-")}
       |${sp}Heart rate:  ${avgHr.getOrElse("-")}
       |""".stripMargin
  }
}

object Summary {
  val empty =
    Summary(0, Distance.zero, Duration.ZERO, None, None, Calories.zero, None, None)

  def summarize(sessions: Vector[ActivitySessionRecord]): Summary =
    Summary.from(
      sessions.map(s =>
        Summary(
          1,
          s.distance,
          s.elapsedTime,
          s.totalAscend,
          s.avgHr,
          s.calories,
          s.maxTemp,
          s.minTemp
        )
      )
    )

  def from(many: Vector[Summary]): Summary = {
    val hrs = many.flatMap(_.avgHr.map(_.bpm))
    val avgHr =
      if (hrs.isEmpty) None
      else
        (hrs.sum.toDouble / hrs.size).toInt.some
          .filter(_ > 0)
    many.foldLeft(empty) { (res, el) =>
      Summary(
        res.count + el.count,
        res.distance + el.distance,
        res.time.plus(el.time),
        res.elevation |+| el.elevation,
        avgHr.map(HeartRate.bpm),
        res.calories + el.calories,
        cats.Order.max(res.maxTemp, el.maxTemp),
        selectMin(res.minTemp, el.minTemp)
      )
    }
  }

  private def selectMin[A](a: Option[A], b: Option[A])(implicit ordering: Ordering[A]) =
    (a, b) match {
      case (Some(x), Some(y)) => ordering.min(x, y).some
      case (x, None)          => x
      case (None, y)          => y
    }
}
