package fit4s.webview.client.search

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.Signal
import fs2.dom.HtmlDivElement

import _root_.fit4s.activities.data.Page
import _root_.fit4s.webview.client.shared.{Anchor, ClickAction}
import calico.html.io.{*, given}

object PagingDiv:
  final case class Model(
      currentPage: Int,
      lastPage: Int,
      page: Page
  ):

    def expand(contextSize: Int): List[Model.Element] =
      val candidates =
        (1 :: currentPageContext(contextSize) ::: List(lastPage)).distinct.sorted.filter(
          p => p > 0 && p <= lastPage
        )
      candidates match
        case Nil => Nil
        case _   => pagingLabels(candidates)

    private def currentPageContext(amount: Int = 2): List[Int] =
      ((currentPage - amount) to (currentPage + amount)).toList.filter(_ >= 1)

    private def pagingLabels(pages: List[Int]): List[Model.Element] =
      (pages.zip(pages.tail).flatMap { case (p, n) =>
        val current = Model.Element.Page(p, p == currentPage)
        if (n - p <= 1) List(current)
        else List(current, Model.Element.Fill)
      }) ::: List(Model.Element.Page(pages.last, pages.last == currentPage))

  object Model:
    enum Element:
      case Fill
      case Page(n: Int, current: Boolean)

    val empty: Model = Model(1, 1, Page.one(25))

    given Eq[Model] = Eq.fromUniversalEquals

  def apply(
      model: Signal[IO, Model],
      onPage: Int => IO[Unit]
  ): Signal[IO, Resource[IO, HtmlDivElement[IO]]] =
    val pages = model.map(_.expand(1))
    val links =
      pages.map(_.traverse {
        case Model.Element.Page(n, true) =>
          span(
            cls := s"px-2 py-1 $currentStyle",
            n.toString
          )
        case Model.Element.Page(n, false) =>
          calico.html.io.a(
            cls := s"px-2 py-1 ${Anchor.style}",
            onClick --> ClickAction(onPage(n)),
            href := "#",
            n.toString
          )
        case Model.Element.Fill =>
          span(cls := "px-2 py-1", "...")
      })

    val styles = pages.map:
      case Nil      => "hidden" :: Nil
      case _ :: Nil => "hidden" :: Nil
      case _        => "flex flex-row space-x-3 items-center justify-right" :: Nil

    links.map(children =>
      div(
        cls <-- styles,
        children
      )
    )

  private val currentStyle = "font-bold underline"
