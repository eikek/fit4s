package fit4s.webview.client.search

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import fit4s.activities.data.*
import fit4s.webview.client.FetchResult
import fit4s.webview.client.cmd.*
import fit4s.webview.client.shared.*

import calico.html.io.{*, given}

object ActivityListPanel:

  final case class Model(
      activities: List[ActivityListResult]
  )
  object Model:
    val empty: Model = Model(Nil)
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)

  enum Msg:
    case SetResults(results: FetchResult[List[ActivityListResult]])

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match
      case Msg.SetResults(results) =>
        (results.fold(Model.apply, _ => model), IO.unit)

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap:
      case Result.SearchResult(cmd, results) =>
        model.flatModify(update(cr, Msg.SetResults(results.map(_._1))))

      case Result.SearchListOnlyResult(cmd, results) =>
        model.flatModify(update(cr, Msg.SetResults(results)))

      case _ => IO.unit

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO], zone: ZoneId) =
    Resource
      .eval(subscribe(model, cr).compile.drain.start)
      .as(
        model.map(m =>
          m.activities
            .traverse(renderCard(_, setDetailPage(cr), setTagSearch(cr), zone))
            .flatMap(divs =>
              div(cls := "flex-grow px-2 mt-4 flex flex-col space-y-2", divs)
            )
        )
      )

  private def setDetailPage(cr: CommandRuntime[IO])(r: ActivityListResult): IO[Unit] =
    cr.send(Cmd.SetDetailPage(r.id))

  private def setTagSearch(cr: CommandRuntime[IO])(tag: Tag): IO[Unit] =
    val q = s"tag=${tag.name.quoted}"
    cr.send(Cmd.SetSearchQuery(q))

  def renderCard(
      a: ActivityListResult,
      activityClick: ActivityListResult => IO[Unit],
      tagClick: Tag => IO[Unit],
      zone: ZoneId
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "card py-4 px-4 border rounded-md dark:border-stone-700 dark:hover:border-sky-700 flex flex-col",
      idAttr := s"card-${a.id.id}",
      div(
        cls := "flex flex-row items-center font-bold text-lg",
        ActivityDateHeader(a, zone),
        div(
          cls := "flex-grow text-xs opacity-50 text-right",
          Anchor.strava(a.stravaId),
          s"${a.activity.device.show}, Id ${a.activity.id.id}"
        )
      ),
      ActivityTitle(a.activity, activityClick(a)),
      TagList(a.tags.toList, Some(tagClick)),
      ActivityNotes(a.activity),
      ActivitySessionDataDiv(a.sessions.head)
    )
