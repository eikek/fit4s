package fit4s.webview.client.search

import cats.Show
import cats.effect.*
import cats.syntax.all.*
import fs2.dom.*

import _root_.fit4s.activities.data.ActivitySession
import _root_.fit4s.common.instances.all.*
import _root_.fit4s.profile.types.Sport
import _root_.fit4s.webview.client.shared.Styles
import calico.*
import calico.html.io.{*, given}

object ActivitySessionDataDiv {
  val distanceIcon = Styles.distanceIcon
  val elevationIcon = Styles.elevationIcon
  val speedIcon = Styles.speedIcon
  val timeIcon = Styles.timeIcon

  implicit def optionShow[A: Show]: Show[Option[A]] =
    Show {
      case Some(a) => a.show
      case None    => "-"
    }

  private def valueBox(iconCls: String, label: String, value: String) =
    div(
      cls := (if (value == "-") "hidden" else "flex flex-col px-2 items-center w-40"),
      div(
        cls := "opacity-50 text-sm",
        i(cls := iconCls),
        label
      ),
      div(cls := "font-mono", value)
    )

  def apply(s: ActivitySession): Resource[IO, HtmlDivElement[IO]] = {
    implicit val sport: Sport = s.sport // for distance.show

    div(
      cls := "flex flex-col divide-y divide-stone-700 divide-dashed space-y-4",
      div(
        cls := "flex flex-col space-y-2",
        div(
          cls := "flex flex-row space-x-2",
          // Distance
          valueBox(s"$distanceIcon mr-1", "Distance", s.distance.show),

          // Elevation
          valueBox(s"$elevationIcon mr-1", "Elevation", s.totalAscend.show)
        ),
        div(
          cls := "flex flex-row space-x-2",
          // moving time
          valueBox(s"$timeIcon mr-1", "Moving", s.movingTime.show),

          // avg speed
          valueBox(s"$speedIcon mr-1", "Avg. Speed", s.avgSpeed.show)
        ),
        div(
          cls := "flex flex-row space-x-2",

          // avg heart rate
          valueBox("fa fa-heart-pulse text-red-400 mr-1", "Avg. HR", s.avgHr.show),

          // norm power
          valueBox("fa fa-bolt text-sky-400 mr-1", "Norm Power", s.normPower.show)
        )
      ),
      div(
        cls := "flex flex-col space-y-2 pt-4",
        div(
          cls := "flex flex-row space-x-2",
          // elapsed time
          valueBox(s"$timeIcon mr-1", "Elapsed", s.elapsedTime.show),

          // Calories
          valueBox("fa fa-fire text-sky-400 mr-1", "Calories", s.calories.show)
        ),
        div(
          cls := "flex flex-row space-x-2",
          // Temperature
          valueBox("fa fa-temperature-low text-sky-400 mr-1", "Min Temp", s.minTemp.show),
          valueBox("fa fa-temperature-high text-red-400 mr-1", "Max Temp", s.maxTemp.show)
        ),
        div(
          cls := "flex flex-row space-x-2",
          valueBox(
            "fa fa-temperature-quarter text-sky-400 mr-1",
            "Avg Temp",
            s.avgTemp.show
          )
        )
      )
    )
  }
}
