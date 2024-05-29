package fit4s.webview.client.shared

import cats.effect.*
import fs2.dom.*

import fit4s.activities.data.Activity

import calico.*
import calico.html.io.{*, given}

object ActivityNotes:

  def apply(a: Activity): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "notes break-word text-md opacity-50 mb-4 dark:border-stone-700",
      a.notes.getOrElse("")
    )
