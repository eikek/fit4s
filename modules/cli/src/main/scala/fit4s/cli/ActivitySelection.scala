package fit4s.cli

import java.time._
import java.time.temporal.ChronoUnit

import cats.data.{NonEmptyList => Nel}
import cats.syntax.all._

import fit4s.activities.ActivityQuery
import fit4s.activities.ActivityQuery.Condition.{And, StartedAfter, StartedBefore}
import fit4s.activities.impl.ConditionParser

sealed trait ActivitySelection extends Product

object ActivitySelection {
  case object NoQuery extends ActivitySelection

  case class ForWeek(back: Option[Int]) extends ActivitySelection

  case class ForYear(year: Option[Int]) extends ActivitySelection

  case class Custom(query: String) extends ActivitySelection

  def makeCondition(
      q: ActivitySelection,
      zoneId: ZoneId,
      currentTime: Instant
  ): Either[String, Option[ActivityQuery.Condition]] =
    q match {
      case ActivitySelection.NoQuery => None.asRight
      case ActivitySelection.ForWeek(None) =>
        Some(StartedAfter(findStartLastMonday(currentTime.atZone(zoneId)))).asRight
      case ActivitySelection.ForWeek(Some(back)) =>
        val last = findStartLastMonday(currentTime.atZone(zoneId))
        val a = last.minus(Duration.ofDays(7 * math.max(1, back)))
        val b = a.plus(Duration.ofDays(7)).minusSeconds(1)
        Some(makeTimeIntervalQuery(a, b)).asRight
      case ActivitySelection.ForYear(None) =>
        Some(makeYearQuery(currentTime.atZone(zoneId).getYear, zoneId)).asRight
      case ActivitySelection.ForYear(Some(y)) =>
        Some(makeYearQuery(y, zoneId)).asRight
      case ActivitySelection.Custom(str) =>
        new ConditionParser(zoneId, currentTime)
          .parseCondition(str)
          .left
          .map(err => s"Query parsing failed: $err")
          .map(_.some)
    }

  def findStartLastMonday(current: ZonedDateTime) = {
    val curDay = current.getDayOfWeek
    val diffDays = curDay.getValue - DayOfWeek.MONDAY.getValue
    current
      .minus(diffDays, ChronoUnit.DAYS)
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .truncatedTo(ChronoUnit.SECONDS)
      .toInstant
  }

  private def makeYearQuery(year: Int, zoneId: ZoneId): ActivityQuery.Condition =
    makeTimeIntervalQuery(
      LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant,
      ZonedDateTime
        .of(
          LocalDate.of(year, 12, 31),
          LocalTime.of(23, 59, 59),
          zoneId
        )
        .toInstant
    )

  private def makeTimeIntervalQuery(
      start: Instant,
      end: Instant
  ): ActivityQuery.Condition =
    And(Nel.of(StartedAfter(start), StartedBefore(end)))
}
