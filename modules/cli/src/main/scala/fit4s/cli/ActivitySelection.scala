package fit4s.cli

import java.time.*

import cats.data.NonEmptyList as Nel
import cats.syntax.all.*

import fit4s.activities.data.QueryCondition
import fit4s.activities.data.QueryCondition.{And, StartedAfter, StartedBefore}
import fit4s.common.util.DateUtil

enum ActivitySelection:
  case NoQuery
  case ForWeek(back: Option[Int])
  case ForYear(year: Option[Int])
  case Custom(query: String)

object ActivitySelection:
  def makeCondition(
      q: ActivitySelection,
      zoneId: ZoneId,
      currentTime: Instant
  ): Either[String, Option[QueryCondition]] =
    q match {
      case ActivitySelection.NoQuery       => None.asRight
      case ActivitySelection.ForWeek(None) =>
        Some(
          StartedAfter(DateUtil.findStartLastMonday(currentTime.atZone(zoneId)))
        ).asRight
      case ActivitySelection.ForWeek(Some(back)) =>
        val (a, b) = DateUtil.findPreviousWeek(currentTime.atZone(zoneId), back)
        Some(makeTimeIntervalQuery(a, b)).asRight
      case ActivitySelection.ForYear(None) =>
        Some(makeYearQuery(currentTime.atZone(zoneId).getYear, zoneId)).asRight
      case ActivitySelection.ForYear(Some(y)) =>
        Some(makeYearQuery(y, zoneId)).asRight
      case ActivitySelection.Custom(str) =>
        QueryCondition
          .parser(zoneId, currentTime)
          .apply(str)
          .left
          .map(err => s"Query parsing failed:\n$err")
          .map(_.some)
    }

  private def makeYearQuery(year: Int, zoneId: ZoneId): QueryCondition =
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
  ): QueryCondition =
    And(Nel.of(StartedAfter(start), StartedBefore(end)))
