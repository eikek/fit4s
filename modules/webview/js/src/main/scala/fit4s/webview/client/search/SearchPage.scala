package fit4s.webview.client.search

import java.time.ZoneId

import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import fit4s.webview.client.cmd.*

import calico.html.io.{*, given}

object SearchPage {

  final case class Model(
      search: SearchPanel.Model,
      list: ActivityListPanel.Model,
      summary: ActivitySummaryPanel.Model,
      bikes: BikeSummaryPanel.Model
  )
  object Model:
    val empty: Model = Model(
      SearchPanel.Model.empty,
      ActivityListPanel.Model.empty,
      ActivitySummaryPanel.Model.empty,
      BikeSummaryPanel.Model.empty
    )
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO], zone: ZoneId) = {
    val searchModel = SignallingRef.lens(model)(
      get = _.search,
      set = a => b => a.copy(search = b)
    )
    val listModel = SignallingRef.lens(model)(
      get = _.list,
      set = a => b => a.copy(list = b)
    )
    val summaryModel = SignallingRef.lens(model)(
      get = _.summary,
      set = a => b => a.copy(summary = b)
    )
    val bikesModel = SignallingRef.lens(model)(_.bikes, a => b => a.copy(bikes = b))

    div(
      cls := "flex flex-col",
      SearchPanel.render(searchModel, cr, zone),
      div(
        cls := "flex flex-row space-x-2",
        ActivityListPanel.render(listModel, cr, zone),
        div(
          cls := "flex flex-col space-y-2 aaaaaa",
          ActivitySummaryPanel.render(summaryModel, cr, zone),
          BikeSummaryPanel.render(bikesModel, cr)
        )
      )
    )
  }
}