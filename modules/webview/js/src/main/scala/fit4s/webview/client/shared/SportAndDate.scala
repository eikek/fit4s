package fit4s.webview.client.shared

import cats.effect.*
import fs2.dom.*

import fit4s.activities.data.ActivitySession

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*

object SportAndDate {

  def apply(a: ActivitySession): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "mr-4 border-b-2 dark:border-sky-500 py-0.5",
      SportIcon(a.sport, moreCls = "mr-2" :: Nil),
      a.startTime.toString
    )
}
