package fit4s.webview.client.search

import java.time.ZoneId

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.*

import fit4s.activities.data.ActivitySessionSummary
import fit4s.webview.client.FetchResult
import fit4s.webview.client.cmd.*

import calico.html.io.{*, given}

object ActivitySummaryPanel:

  final case class Model(
      summaries: List[ActivitySessionSummary]
  )
  object Model:
    val empty: Model = Model(Nil)
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)
    given Eq[Model] = Eq.fromUniversalEquals

  enum Msg:
    case SetResults(results: FetchResult[List[ActivitySessionSummary]])

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match
      case Msg.SetResults(results) =>
        (results.fold(Model.apply, _ => model), IO.unit)

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap:
      case Result.SearchResult(cmd, results) =>
        model.flatModify(update(cr, Msg.SetResults(results.map(_._2))))

      case _ => IO.unit

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO], zone: ZoneId) =
    Resource
      .eval(subscribe(model, cr).compile.drain.start)
      .as(
        model.changes.map(m =>
          m.summaries
            .sortBy(-_.count)
            .traverse(ActivitySessionSummaryDiv(_, zone))
            .flatMap(divs => div(cls := "mt-4 flex flex-col space-y-2", divs))
        )
      )
