package fit4s.webview.client.shared

import cats.effect.*
import fs2.dom.*

import fit4s.activities.data.Activity

import calico.*
import calico.html.io.{*, given}

object ActivityTitle:

  def apply(a: Activity, clicked: IO[Unit]): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "my-4",
      calico.html.io.a(
        cls := "title font-bold text-2xl cursor-pointer dark:hover:text-stone-300",
        onClick --> ClickAction(clicked),
        a.name
      )
    )

  def apply(name: String, clicked: IO[Unit]): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "my-4",
      calico.html.io.a(
        cls := "title font-bold text-2xl cursor-pointer dark:hover:text-stone-300",
        onClick --> ClickAction(clicked),
        name
      )
    )
