package fit4s.cli.activity

import cats.data.{NonEmptyList => Nel}
import cats.effect.{Clock, ExitCode, IO}
import cats.syntax.all._
import fit4s.activities.ActivityQuery.Condition.{And, StartedAfter, StartedBefore}
import fit4s.activities.impl.ConditionParser
import fit4s.activities.{ActivityLog, ActivityQuery}
import fit4s.cli.CliError

import java.time.temporal.ChronoUnit
import java.time._

object SummaryCmd {
  sealed trait SummaryQuery extends Product
  object SummaryQuery {
    case object NoQuery extends SummaryQuery
    case class ForWeek(back: Option[Int]) extends SummaryQuery
    case class ForYear(year: Option[Int]) extends SummaryQuery
    case class Custom(query: String) extends SummaryQuery
  }

  def makeCondition(
      q: SummaryQuery,
      zoneId: ZoneId,
      currentTime: Instant
  ): Either[String, Option[ActivityQuery.Condition]] =
    q match {
      case SummaryQuery.NoQuery => None.asRight
      case SummaryQuery.ForWeek(None) =>
        Some(StartedAfter(findStartLastMonday(currentTime.atZone(zoneId)))).asRight
      case SummaryQuery.ForWeek(Some(back)) =>
        val last = findStartLastMonday(currentTime.atZone(zoneId))
        val a = last.minus(Duration.ofDays(7 * math.max(1, back)))
        val b = a.plus(Duration.ofDays(7)).minusSeconds(1)
        Some(makeTimeIntervalQuery(a, b)).asRight
      case SummaryQuery.ForYear(None) =>
        Some(makeYearQuery(currentTime.atZone(zoneId).getYear, zoneId)).asRight
      case SummaryQuery.ForYear(Some(y)) =>
        Some(makeYearQuery(y, zoneId)).asRight
      case SummaryQuery.Custom(str) =>
        new ConditionParser(zoneId, currentTime)
          .parseCondition(str)
          .left
          .map(err => s"Query parsing failed: $err")
          .map(_.some)
    }

  final case class Config(
      query: SummaryQuery
  )

  def apply(cfg: Config): IO[ExitCode] =
    ActivityLog.default[IO]().use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        zone = ZoneId.systemDefault()

        query <- makeCondition(cfg.query, zone, currentTime)
          .fold(err => IO.raiseError(new CliError(err)), IO.pure)

        summary <- log.activitySummary(query)
        out = summary.map(ConsoleUtil.makeSummaryTable(2, zone))

        _ <- out.traverse_ { case (head, data) =>
          IO.println(head) *> IO.println("\n") *> IO.println(data) *> IO.println("\n")
        }
      } yield ExitCode.Success
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
