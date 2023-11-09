package fit4s.webview.client.shared

import cats.effect.*
import cats.syntax.all.*
import fs2.dom.*

import fit4s.strava.data.StravaActivityId

import calico.*
import calico.html.io.{*, given}

object Anchor {
  val style =
    "text-blue-400 hover:text-blue-500 dark:text-sky-300 dark:hover:text-sky-200 cursor-pointer"

  def apply(ref: Option[String], label: String): Resource[IO, HtmlAnchorElement[IO]] =
    calico.html.io.a(
      cls := ref.fold("hidden")(_ => s"mr-2 $style"),
      href := ref.getOrElse(""),
      label
    )

  def strava(id: Option[StravaActivityId]) =
    apply(
      id.map(n => s"https://strava.com/activities/${n.id}"),
      "Strava"
    )

  def stravaId(id: StravaActivityId) =
    apply(
      s"https://strava.com/activities/${id.id}".some,
      s"${id.id}"
    )
}
