package fit4s.cli.activity

import cats.data.{NonEmptyList => Nel}
import cats.effect.{Clock, ExitCode, IO}
import cats.syntax.all._
import fit4s.activities.ActivityQuery.Condition._
import fit4s.activities.ActivityQuery.OrderBy
import fit4s.activities.data.TagName
import fit4s.activities.records.ActivitySessionRecord
import fit4s.activities.{ActivityLog, ActivityQuery}
import fit4s.profile.types.Sport

import java.time.temporal.ChronoUnit
import java.time.{DayOfWeek, ZoneId, ZonedDateTime}

object WeekSummaryCmd {

  final case class Config(
      sport: Option[Sport],
      excludeTags: List[TagName],
      includeTags: List[TagName]
  )

  def apply(cfg: Config): IO[ExitCode] =
    ActivityLog.default[IO]().use { log =>
      for {
        currentTime <- Clock[IO].realTimeInstant
        monday = findStartLastMonday(currentTime.atZone(ZoneId.systemDefault()))
        query = And(
          Nel(
            StartedAfter(monday),
            cfg.sport.toList.map(SportMatch) :::
              Nel.fromList(cfg.excludeTags).map(TagAnyMatch).map(Not).toList :::
              Nel.fromList(cfg.includeTags).map(TagAnyMatch).toList
          )
        )
        sessions <- log
          .activityList(ActivityQuery(Some(query), OrderBy.StartTime))
          .compile
          .toVector

        _ <- IO.println(ConsoleUtil.printHeader("This Week"))
        _ <- IO.println(Summary.summarize(sessions).summaryTable(2))

        table = makeTable(ZoneId.systemDefault(), sessions)
        rows =
          table.toList
            .sortBy(_._1)
            .flatMap { case (dow, bySport) =>
              ConsoleUtil.printHeader("\n" + dow.toString) ::
                bySport.flatMap { case (sport, summary) =>
                  (" " + sport.toString) :: summary.summaryTable(4) :: Nil
                }.toList
            }

        _ <- rows.traverse_(IO.println)
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

  def makeTable(zoneId: ZoneId, sessions: Vector[ActivitySessionRecord]) = {
    val byWeekday = sessions.groupBy(_.startTime.atZone(zoneId).getDayOfWeek)
    byWeekday.view
      .mapValues(_.groupBy(_.sport).view.mapValues(Summary.summarize).toMap)
      .toMap
  }
}
