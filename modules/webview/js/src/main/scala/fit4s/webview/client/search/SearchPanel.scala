package fit4s.webview.client.search

import java.time.ZoneId

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import fit4s.activities.data.{ActivitySessionSummary, Page, Tag}
import fit4s.webview.client.FetchResult
import fit4s.webview.client.cmd.*
import fit4s.webview.client.shared.ErrorPanel
import fit4s.webview.data.RequestFailure

import calico.html.io.{*, given}

object SearchPanel:
  final case class Model(
      search: SearchField.Model,
      paging: PagingDiv.Model,
      errors: Option[RequestFailure],
      bikeTags: List[Tag],
      shoeTags: List[Tag]
  )
  object Model:
    val empty = Model(SearchField.Model.empty, PagingDiv.Model.empty, None, Nil, Nil)
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)

  enum Msg:
    case SetCurrentPage(n: Int)
    case SetResults(r: FetchResult[List[ActivitySessionSummary]], page: Page)
    case Search(query: String)
    case SetSearch(query: String)
    case SetBikeTags(tags: FetchResult[List[Tag]])
    case SetShoeTags(tags: FetchResult[List[Tag]])
    case RefreshSearch

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match
      case Msg.SetCurrentPage(n) =>
        val p = Page(model.paging.page.limit, (n - 1) * model.paging.page.limit)
        val cmd1 = cr.send(Cmd.SearchListOnlyCmd(model.search.text.text, p))
        (
          model.copy(
            paging = model.paging.copy(currentPage = n, page = p),
            search = model.search.setBusy
          ),
          cmd1
        )

      case Msg.SetResults(result, page) =>
        val next =
          result.fold(
            r => {
              val all = r.map(_.count).sum
              val lastPage: Int =
                (all.toDouble / model.paging.page.limit.toDouble).ceil.toInt
              model.copy(
                search = model.search.unsetBusy,
                paging = model.paging.copy(lastPage = lastPage, page = page),
                errors = None
              )
            },
            err => model.copy(errors = err.some, search = model.search.unsetBusy)
          )
        (next, IO.unit)

      case Msg.SetSearch(q) =>
        val cmd1 = cr.send(Cmd.SearchCmd(q, PagingDiv.Model.empty.page))
        val cmd2 = cr.send(Cmd.SearchTagSummary(q, model.bikeTags))
        val cmd3 = cr.send(Cmd.SearchTagSummary(q, model.shoeTags))
        (model.copy(search = model.search.setText(q).setBusy), cmd1 >> cmd2 >> cmd3)

      case Msg.Search(q) =>
        val cmd1 = cr.send(Cmd.SearchCmd(q, PagingDiv.Model.empty.page))
        val cmd2 = cr.send(Cmd.SearchTagSummary(q, model.bikeTags))
        val cmd3 = cr.send(Cmd.SearchTagSummary(q, model.shoeTags))
        (
          model.copy(search = model.search.setBusy, paging = PagingDiv.Model.empty),
          cmd1 >> cmd2 >> cmd3
        )

      case Msg.RefreshSearch =>
        val cmd = Cmd.SearchListOnlyCmd(model.search.text.text, page = model.paging.page)
        (model.copy(search = model.search.setBusy), cr.send(cmd))

      case Msg.SetBikeTags(result) =>
        val next = result.fold(
          r => model.copy(bikeTags = r),
          errs => {
            scribe.error(s"Error getting bike tags: $errs")
            model
          }
        )
        (next, IO.unit)

      case Msg.SetShoeTags(result) =>
        val next = result.fold(
          r => model.copy(shoeTags = r),
          errs => {
            scribe.error(s"Error getting shoe tags: $errs")
            model
          }
        )
        (next, IO.unit)

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap:
      case Result.SearchResult(cmd, results) =>
        model.flatModify(update(cr, Msg.SetResults(results.map(_._2), cmd.page)))

      case Result.SearchListOnlyResult(cmd, results) =>
        model.update(m => m.copy(search = m.search.unsetBusy))

      case Result.GetBikeTagsResult(result) =>
        model.flatModify(update(cr, Msg.SetBikeTags(result)))

      case Result.GetShoeTagsResult(result) =>
        model.flatModify(update(cr, Msg.SetShoeTags(result)))

      case Result.SetSearchQueryResult(q) =>
        model.flatModify(update(cr, Msg.SetSearch(q)))

      case Result.SearchRefresh =>
        model.flatModify(update(cr, Msg.RefreshSearch))

      case _ => IO.unit

  def render(
      model: SignallingRef[IO, Model],
      cr: CommandRuntime[IO],
      zoneId: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    val searchModel = SignallingRef.lens(model)(
      get = _.search,
      set = a => b => a.copy(search = b)
    )
    Resource.eval(subscribe(model, cr).compile.drain.start) *>
      Resource.eval(cr.send(Cmd.GetBikeTags)) *>
      Resource.eval(cr.send(Cmd.GetShoeTags)) *>
      div(
        cls := "flex flex-col space-y-2",
        SearchField(
          searchModel,
          q => model.flatModify(update(cr, Msg.Search(q)))
        ),
        div(
          cls := "flex flex-row space-x-2",
          DefinedQueries(q => model.flatModify(update(cr, Msg.SetSearch(q))), zoneId),
          PagingDiv(
            model.map(_.paging).changes,
            n => model.flatModify(update(cr, Msg.SetCurrentPage(n)))
          )
        ),
        model.map(_.errors).changes.map {
          case Some(err) => ErrorPanel(err)
          case None      => span(cls := "hidden")
        }
      )
