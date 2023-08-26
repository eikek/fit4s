package fit4s.webview.client

import java.time.ZoneId

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlElement

import fit4s.webview.client.cmd.*
import fit4s.webview.client.detail.DetailPage
import fit4s.webview.client.search.SearchPage

import calico.IOWebApp
import calico.html.io.{*, given}
import org.http4s.implicits.*

object Fit4sApp extends IOWebApp {
  private[this] val logger = scribe.Logger("Fit4sApp")
  private[this] val loggerF = scribe.cats.io

  val zoneId: ZoneId =
    try ZoneId.systemDefault()
    catch
      case e =>
        logger.warn(
          s"Cannot load system time zone: ${e.getMessage}. Use Europe/Berlin as fallback"
        )
        ZoneId.of("Europe/Berlin")

  // the base url is injected at build time
  val fit4sClient = Fit4sClient.create[IO](BaseUrl.apply.get)

  enum Page:
    case SearchPage
    case DetailPage
  object Page:
    given Eq[Page] = Eq.fromUniversalEquals

  final case class Model(
      page: Page,
      searchPage: SearchPage.Model,
      detailPage: Option[DetailPage.Model]
  ) {
    def setDetail(dm: Option[DetailPage.Model]): Model =
      dm.map(_ => copy(detailPage = dm, page = Page.DetailPage)).getOrElse(this)

    def setSearch: Model =
      copy(detailPage = None, page = Page.SearchPage)
  }
  object Model:
    val empty: Model = Model(Page.SearchPage, SearchPage.Model.empty, None)
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap {
      case Result.DetailResult(result) =>
        result.fold(
          d => model.update(_.setDetail(d.map(DetailPage.Model.make))),
          err => loggerF.error(s"Error getting detail: $err")
        )

      case Result.SetSearchPageResult =>
        model.update(_.setSearch)

      case _ => IO.unit
    }

  override def render: Resource[IO, HtmlElement[IO]] =
    for {
      model <- Resource.eval(Model.makeEmpty)
      cr <- Resource.eval(CommandRuntime[IO](fit4sClient))
      _ <- Resource.eval(subscribe(model, cr).compile.drain.start)

      searchModel = SignallingRef.lens(model)(
        _.searchPage,
        a => b => a.copy(searchPage = b)
      )
      detailModel = SignallingRef.lens(model)(
        _.detailPage.getOrElse(sys.error("no detail")), // TODO improve here
        a => b => a.copy(detailPage = b.some)
      )

      cnt <-
        div(
          cls := "parent",
          model.map(_.page).changes.map {
            case Page.DetailPage => DetailPage.render(detailModel, cr, zoneId)
            case Page.SearchPage => SearchPage.render(searchModel, cr, zoneId)
          }
        )
    } yield cnt
}
