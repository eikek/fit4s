package fit4s.webview.client.search

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.dom.*

import fit4s.activities.data.ActivitySessionSummary
import fit4s.common.instances.all.*
import fit4s.profile.types.Sport
import fit4s.webview.client.shared.SportIcon
import fit4s.webview.client.util.FormatDate

import calico.*
import calico.html.io.{*, given}

object ActivitySessionSummaryDiv:
  val cardStyle =
    "card py-4 px-4 border rounded-md dark:border-stone-700 dark:hover:border-sky-700 flex flex-col"
  val labelStyle = "opacity-75 text-sm"
  val valueStyle = "font-mono"
  val headerStyle =
    "col-span-3 text-lg font-bold border border-t-0 border-x-0 border-b-2 dark:border-sky-500 py-0.5 mb-1"

  def apply(a: ActivitySessionSummary, zone: ZoneId): Resource[IO, HtmlDivElement[IO]] =
    implicit val sport: Sport = a.sport

    div(
      cls := cardStyle,
      div(
        cls := "grid grid-cols-3 gap-x-4 gap-y-1",
        div(
          cls := headerStyle,
          span(
            SportIcon(a.sport, "mr-2" :: Nil),
            a.sport.show
          )
        ),

        // Number of activities
        div(cls := labelStyle, "Activities"),
        div(cls := s"col-span-2 $valueStyle", a.count.toString),

        // Date
        div(
          cls := labelStyle,
          "Date"
        ),
        div(
          cls := s"col-span-2 break-keep $valueStyle",
          s"${FormatDate(a.startTime, zone)} — ${FormatDate(a.endTime, zone)}"
        ),

        // Moving
        makeRow(!a.movingTime.isZero)(
          div(cls := labelStyle, "Moved"),
          div(cls := s"col-span-2 $valueStyle", a.movingTime.show)
        ),

        // Distance
        div(cls := labelStyle, "Distance"),
        div(cls := s"col-span-2 $valueStyle", a.distance.show),

        // Ascend
        makeRow(a.totalAscend.isDefined)(
          div(cls := labelStyle, "Elevation"),
          div(cls := s"col-span-2 $valueStyle", a.totalAscend.show)
        ),

        // Calories
        makeRow(a.calories.isPresent)(
          div(cls := labelStyle, "Calories"),
          div(cls := s"col-span-2 $valueStyle", a.calories.show)
        ),

        // Avg Speed
        makeRow(a.avgSpeed.isDefined)(
          div(cls := labelStyle, "Avg. Speed"),
          div(cls := s"col-span-2 $valueStyle", a.avgSpeed.show)
        ),

        // Heart Rate
        makeRow(List(a.minHr, a.maxHr, a.avgHr).exists(_.isDefined))(
          div(cls := labelStyle, "Heart Rate"),
          div(
            cls := s"col-span-2 $valueStyle",
            s"${a.minHr.show}-${a.maxHr.show}, ${a.avgHr.show}"
          )
        ),

        // Temperature
        makeRow(List(a.minTemp, a.maxTemp, a.avgTemp).exists(_.isDefined))(
          div(cls := labelStyle, "Temperature"),
          div(
            cls := s"col-span-2 $valueStyle",
            s"${a.minTemp.show}-${a.maxTemp.show}, ${a.avgTemp.show}"
          )
        )
      )
    )

  def makeRow(
      enabled: Boolean
  )(els: Resource[IO, HtmlDivElement[IO]]*): List[Resource[IO, HtmlDivElement[IO]]] =
    if (!enabled) Nil
    else els.toList
