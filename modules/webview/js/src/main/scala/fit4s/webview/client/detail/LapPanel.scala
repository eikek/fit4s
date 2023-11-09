package fit4s.webview.client.detail

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.dom.HtmlElement

import fit4s.activities.data.{ActivityDetailResult, ActivityLap}
import fit4s.common.syntax.all.*
import fit4s.profile.types.Sport

import calico.html.io.{*, given}

object LapPanel {
  private val cellStyle = " text-center"

  def apply(
      details: ActivityDetailResult,
      zone: ZoneId
  ): Resource[IO, HtmlElement[IO]] = {
    val s = details.sessions.head
    val laps = details.laps.get(s.id).getOrElse(Nil)
    apply(laps, zone)
  }

  def apply(laps: List[ActivityLap], zone: ZoneId): Resource[IO, HtmlElement[IO]] = {
    implicit val zoneId: ZoneId = zone
    implicit val styles = HasValue(laps)
    div(
      cls := "",
      table(
        cls := "w-full table-auto border-collapse",
        thead(
          cls := "border-b dark:border-stone-700",
          tr(
            th(cls := cellStyle, ""),
            th(cls := cellStyle, "Time"),
            th(cls := styles.get(_.moved, cellStyle), "Moved"),
            th(cls := cellStyle, "Distance"),
            th(cls := styles.get(_.avgSpeed, cellStyle), "Avg.Speed"),
            th(cls := styles.get(_.avgCadence, cellStyle), "Avg.Cad"),
            th(cls := styles.get(_.avgHr, cellStyle), "Avg.Hr"),
            th(cls := styles.get(_.avgPower, cellStyle), "Avg.Power"),
            th(cls := styles.get(_.swimStroke, cellStyle), "Stroke"),
            th(cls := styles.get(_.strokeCount, cellStyle), "Stroke Count"),
            th(cls := styles.get(_.avgStrokeDst, cellStyle), "Stroke Dst")
          )
        ),
        tbody(
          laps.zipWithIndex.traverse(row.tupled)
        )
      )
    )
  }

  def row(l: ActivityLap, n: Int)(implicit zone: ZoneId, styles: HasValue) = {
    implicit val sport: Sport = l.sport
    tr(
      cls := "dark:hover:bg-stone-600 hover:bg-slate-200",
      td(cls := cellStyle, (n + 1).toString),
      td(cls := cellStyle, l.startTime.asTime.show),
      td(cls := styles.get(_.moved, cellStyle), l.movingTime.show),
      td(cls := cellStyle, l.distance.show),
      td(cls := styles.get(_.avgSpeed, cellStyle), l.avgSpeed.show),
      td(cls := styles.get(_.avgCadence, cellStyle), l.avgCadence.show),
      td(cls := styles.get(_.avgHr, cellStyle), l.avgHr.show),
      td(cls := styles.get(_.avgPower, cellStyle), l.avgPower.show),
      td(cls := styles.get(_.swimStroke, cellStyle), l.swimStroke.show),
      td(cls := styles.get(_.strokeCount, cellStyle), l.strokeCount.show),
      td(cls := styles.get(_.avgStrokeDst, cellStyle), l.avgStrokeDistance.show)
    )
  }

  final case class HasValue(
      moved: Boolean,
      avgSpeed: Boolean,
      avgCadence: Boolean,
      avgHr: Boolean,
      avgPower: Boolean,
      swimStroke: Boolean,
      strokeCount: Boolean,
      avgStrokeDst: Boolean
  ) {
    def update(l: ActivityLap): HasValue =
      copy(
        moved = this.moved || l.movingTime.isDefined,
        avgSpeed = this.avgSpeed || l.avgSpeed.isDefined,
        avgHr = this.avgHr || l.avgHr.isDefined,
        avgPower = this.avgPower || l.avgPower.isDefined,
        swimStroke = this.swimStroke || l.swimStroke.isDefined,
        strokeCount = this.strokeCount || l.strokeCount.isDefined,
        avgStrokeDst = this.avgStrokeDst || l.avgStrokeDistance.isDefined
      )

    def get(f: HasValue => Boolean, v: String): String =
      if (f(this)) v else "hidden"
  }

  object HasValue {
    val empty: HasValue = HasValue(false, false, false, false, false, false, false, false)

    def apply(laps: List[ActivityLap]): HasValue =
      laps.foldLeft(empty)(_ update _)
  }
}
