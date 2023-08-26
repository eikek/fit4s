package fit4s.webview.client.detail

import cats.Eq
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.dom.HtmlElement

import fit4s.activities.data.{ActivityDetailResult, ActivitySession}
import fit4s.cats.instances.all.*
import fit4s.profile.types.Sport
import fit4s.webview.client.shared.*

import calico.html.io.{*, given}

object DataOverviewPanel {

  def apply(details: ActivityDetailResult) = {
    val s = details.sessions.head
    div(
      cls := "ml-4 flex flex-col self-end",
      div(
        cls := "flex flex-row space-x-8",
        bigValue("Distance", s.distance.show),
        bigValue("Moving Time", s.movingTime.show),
        bigValue("Elevation", s.totalAscend.show),
        bigValue("Calories", s.calories.show)
      ),
      div(
        cls := "flex flex-row space-x-6 mt-4",
        smallValue("Elapsed Time", s.elapsedTime.show),
        smallValue("Descend", s.totalDescend.show),
        smallValue("Norm. Power", s.normPower.show),
        smallValue("TSS", s.tss.show.dropRight(3)),
        smallValue("IF", s.iff.show.dropRight(2)),
        smallValue("Type", s.subSport.show)
      ),
      div(
        cls := "flex flex-row space-x-6 mt-2",
        smallValue("Device", details.activity.device.name),
        smallValue("Id", details.activity.id.show),
        smallValue(
          "File Id",
          details.activity.activityFileId.asString
        ), // TODO this takes very long in the browser!
        smallValue1("Strava", details.stravaId.map(Anchor.stravaId))
      ),

      // min/max table
      dataTable(s)
    )
  }

  def dataTable(s: ActivitySession) = {
    implicit val sport: Sport = s.sport
    table(
      cls := "table-auto mt-2 dark:bg-stone-800 py-2 px-1",
      thead(
        tr(
          cls := "font-bold text-md border-b dark:border-stone-700",
          th(cls := "pl-2 border-r dark:border-stone-700", ""),
          th(cls := "px-2", "Avg"),
          th(cls := "px-2", "Min"),
          th(cls := "px-2", "Max")
        )
      ),
      tbody(
        cls := "",
        tableRow("Heart Rate", s.avgHr.show, s.minHr.show, s.maxHr.show),
        tableRow("Speed", s.avgSpeed.show, "", s.maxSpeed.show),
        tableRow("Temperature", s.avgTemp.show, s.minTemp.show, s.maxTemp.show),
        tableRow("Cadence", s.avgCadence.show, "", s.maxCadence.show),
        tableRow("Power", s.avgPower.show, "", s.maxPower.show),
        tableRow("Stroke Count", s.avgStrokeCount.show, "", ""),
        tableRow("Stroke Distance", s.avgStrokeDistance.show, "", "")
      )
    )
  }

  def tableRow(label: String, avg: String, min: String, max: String) =
    if (List(avg, min, max).forall(_.isEmpty)) div(cls := "hidden")
    else
      tr(
        cls := "text-md",
        td(cls := "pl-2 border-r dark:border-stone-700", label),
        td(cls := "px-2", avg),
        td(cls := "px-2", min),
        td(cls := "px-2", max)
      )

  def bigValue(label: String, value: String) =
    if (value.isEmpty) div(cls := "hidden")
    else
      div(
        cls := "flex flex-col",
        div(
          cls := "opacity-75 text-xs",
          label
        ),
        div(
          cls := "font-bold text-2xl",
          value
        )
      )

  def smallValue(label: String, value: String) =
    if (value.isEmpty) div(cls := "hidden")
    else
      div(
        cls := "flex flex-col",
        div(
          cls := "opacity-75 text-xs",
          label
        ),
        div(
          cls := "text-ellipsis overflow-hidden max-w-xs",
          value
        )
      )

  def smallValue1(label: String, value: Option[Resource[IO, HtmlElement[IO]]]) =
    value match
      case Some(v) =>
        div(
          cls := "flex flex-col",
          div(cls := "opacity-75 text-xs", label),
          div(cls := "", v)
        )
      case None =>
        span(cls := "hidden")

}
