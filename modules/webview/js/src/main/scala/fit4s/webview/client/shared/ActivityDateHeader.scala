package fit4s.webview.client.shared

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import cats.effect.*
import fs2.dom.*

import fit4s.activities.data.Activity
import fit4s.activities.data.ActivityListResult
import fit4s.webview.client.util.FormatTimestamp

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*

object ActivityDateHeader {

  def apply(a: ActivityListResult, zone: ZoneId): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "mr-4 border border-t-0 border-x-0 border-b-2 dark:border-sky-500 py-0.5",
      SportIcon.of(a.sessions.map(_.sport), moreCls = "text-4xl mr-2" :: Nil),
      FormatTimestamp(a.activity.created.getOrElse(a.activity.timestamp), zone)
    )

}
