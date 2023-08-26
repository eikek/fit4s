package fit4s.webview.client.shared

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import cats.effect.*
import fs2.dom.*

import fit4s.webview.data.RequestFailure

import calico.*
import calico.html.io.{*, given}
import calico.syntax.*

object ErrorPanel {

  def apply(errs: RequestFailure): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "flex flex-col space-y-2 dark:text-red-500 mx-4",
      div(
        cls := "text-md",
        errs.message
      ),
      div(
        cls := "px-8 font-mono",
        errs.errors.map(m => li(m))
      )
    )
}
