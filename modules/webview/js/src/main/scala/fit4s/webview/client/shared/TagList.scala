package fit4s.webview.client.shared

import cats.effect.*
import cats.syntax.all.*
import fs2.dom.*

import fit4s.activities.data.Tag

import calico.*
import calico.html.io.{*, given}

object TagList:
  val skyTagStyle =
    "border text-white bg-sky-400 dark:bg-sky-500 dark:text-sky-400 dark:bg-opacity-30 border-sky-300 dark:border-sky-400 dark:bg-opacity-50 flex flex-row items-center px-2 py-0.5 rounded"

  def apply(
      tags: List[Tag],
      action: Option[Tag => IO[Unit]] = None
  ): Resource[IO, HtmlDivElement[IO]] =
    tags
      .traverse(makeTag(action))
      .flatMap(children =>
        div(
          cls := "tags flex flex-row space-x-2 mb-4 items-center",
          children
        )
      )

  def makeTag(action: Option[Tag => IO[Unit]])(
      tag: Tag
  ): Resource[IO, HtmlElement[IO]] =
    action match
      case None =>
        div(
          cls := skyTagStyle,
          tag.name.name
        )
      case Some(act) =>
        calico.html.io.a(
          cls := s"$skyTagStyle cursor-pointer",
          onClick --> ClickAction(act(tag)),
          tag.name.name
        )
