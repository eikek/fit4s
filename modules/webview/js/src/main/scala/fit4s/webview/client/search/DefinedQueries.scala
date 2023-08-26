package fit4s.webview.client.search

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

import cats.Show
import cats.effect.*
import cats.syntax.all.*
import fs2.dom.*

import _root_.fit4s.activities.data.Activity
import _root_.fit4s.activities.data.ActivitySession
import _root_.fit4s.cats.instances.all.*
import _root_.fit4s.common.util.DateUtil
import _root_.fit4s.profile.types.Sport
import _root_.fit4s.webview.client.shared.{Anchor, ClickAction}
import calico.*
import calico.html.io.{*, given}
import calico.syntax.*

object DefinedQueries {

  val queries = List(
    "This week" -> "started>{current_monday}",
    "Last week" -> "started>{last_monday} started<{current_monday}",
    "This month" -> "started>{current_month}",
    "Last month" -> "started>{last_month} started<{current_month}",
    "This year" -> "started>{current_year}",
    "Last year" -> "started>{last_year} started<{current_year}",
    "Clear" -> ""
  )

  def apply(
      setQuery: String => IO[Unit],
      zone: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] = for {
    dateData <- Resource.eval(DateData.make(zone))
    inner <- queries.traverse { case (label, templ) =>
      val q = dateData.replace(templ)
      div(
        cls := "px-2",
        calico.html.io.a(
          href := "#",
          cls := Anchor.style,
          onClick --> ClickAction(setQuery(q)),
          label
        )
      )
    }
    cnt <- div(
      cls := "flex flex-row flex-grow items-center divide-x divide-solid dark:divide-stone-700",
      inner
    )
  } yield cnt

  final case class DateData(
      currentYear: Int,
      lastYear: Int,
      currentMonth: LocalDate,
      lastMonth: LocalDate,
      currentMonday: LocalDate,
      lastMonday: LocalDate
  ) {
    def replace(in: String): String =
      in.replace("{current_year}", currentYear.toString)
        .replace("{last_year}", lastYear.toString)
        .replace("{current_monday}", currentMonday.toString)
        .replace("{last_monday}", lastMonday.toString)
        .replace("{current_month}", currentMonth.toString)
        .replace("{last_month}", lastMonth.toString)
  }
  object DateData:
    def make(zone: ZoneId): IO[DateData] =
      for {
        currentTime <- IO(Instant.now.atZone(zone))
        year = currentTime.getYear
        prevMon = DateUtil.findPreviousWeek(currentTime, 1)
        curMonth = currentTime.withDayOfMonth(1).toLocalDate()
        lastMonth = currentTime.withDayOfMonth(1).minusMonths(1).toLocalDate()
      } yield DateData(
        year,
        year - 1,
        curMonth,
        lastMonth,
        prevMon._2.atZone(zone).toLocalDate().plusDays(1),
        prevMon._1.atZone(zone).toLocalDate()
      )
}
