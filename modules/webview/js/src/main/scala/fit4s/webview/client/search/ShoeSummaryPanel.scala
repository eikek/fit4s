package fit4s.webview.client.search

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef
import fs2.dom.*

import _root_.fit4s.activities.data.{ActivitySessionSummary, Tag}
import _root_.fit4s.common.instances.all.*
import _root_.fit4s.profile.types.Sport
import _root_.fit4s.webview.client.cmd.*
import _root_.fit4s.webview.client.shared.Styles
import calico.*
import calico.html.io.{*, given}

object ShoeSummaryPanel {
  implicit private val sport: Sport = Sport.Running // for speed.show

  private val enabledSports: Set[Sport] =
    Set(Sport.Running, Sport.Walking)

  final case class Model(
      data: List[(Tag, ActivitySessionSummary)],
      loading: Boolean
  )
  object Model:
    val empty: Model = Model(Nil, false)
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)

  enum Msg:
    case SetData(data: List[(Tag, ActivitySessionSummary)])

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match
      case Msg.SetData(data) =>
        (model.copy(data = data), IO.unit)

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap {
      case Result.TagSummaryResult(req, result) if req.hasShoeTags =>
        val data = result.fold(
          _.filter(_._1.name.startsWith1("Shoe/"))
            .flatMap { case (tag, summaries) =>
              NonEmptyList
                .fromList(summaries.filter(e => enabledSports.contains(e.sport)))
                .map(ActivitySessionSummary.combine)
                .map(as => tag -> as)
            },
          err => {
            scribe.error(s"Error getting tag summary: $err")
            Nil
          }
        )
        model.flatModify(update(cr, Msg.SetData(data)))

      case _ =>
        IO.unit
    }

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    Resource
      .eval(subscribe(model, cr).compile.drain.start)
      .as(
        model.map(m =>
          m.data
            .traverse(renderLine.tupled)
            .flatMap(inner =>
              div(
                cls := (if (m.data.isEmpty) "hidden" :: Nil
                        else ActivitySessionSummaryDiv.cardStyle :: List("space-y-2")),
                div(
                  cls := ActivitySessionSummaryDiv.headerStyle,
                  span(
                    i(cls := "fa fa-shoe-prints mr-2"),
                    "Shoes"
                  )
                ),
                inner
              )
            )
        )
      )

  def renderLine(
      tag: Tag,
      sum: ActivitySessionSummary
  ): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "grid grid-cols-5 gap-x-2 gap-y-1 items-center justify-center",
      div(
        cls := ActivitySessionSummaryDiv.labelStyle,
        tag.name.name.stripPrefix("Shoe/")
      ),
      div(
        cls := "col-span-4 font-mono opacity-75 text-sm",
        div(
          cls := "flex flex-row space-x-1",
          div(
            i(cls := Styles.countIcon :: List("mr-1")),
            sum.count.show
          ),
          div(
            i(cls := Styles.distanceIcon :: List("mr-1")),
            sum.distance.show
          ),
          div(
            i(cls := Styles.elevationIcon :: List("mr-1")),
            sum.totalAscend.show
          ),
          div(
            i(cls := Styles.speedIcon :: List("mr-1")),
            sum.avgSpeed.show
          ),
          div(
            i(cls := Styles.timeIcon :: List("mr-1")),
            sum.movingTime.show
          )
        )
      )
    )
}
