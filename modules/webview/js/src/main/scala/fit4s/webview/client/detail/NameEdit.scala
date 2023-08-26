package fit4s.webview.client.shared

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import calico.html.io.{*, given}
import fit4s.webview.client.cmd.*
import fit4s.activities.data.ActivityId

object NameEdit {

  sealed trait Model {
    def activityId: ActivityId
    def name: String
    def editMode: Boolean
  }
  object Model:
    enum EditState {
      case Init
      case Updating
      case Failure
    }
    object EditState {
      given Eq[EditState] = Eq.fromUniversalEquals
    }
    case class View(activityId: ActivityId, name: String) extends Model {
      val editMode = false
    }
    case class Edit(activityId: ActivityId, name: String, prev: String, state: EditState)
        extends Model {
      val editMode = true
    }
    def of(activityId: ActivityId, name: String): Model =
      View(activityId, name)

  enum Msg:
    case ToEdit
    case CancelEdit
    case SaveEdit
    case SetName(name: String)
    case ToView(name: String)
    case ToEditFailed

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match {
      case Msg.ToEdit =>
        (
          Model.Edit(model.activityId, model.name, model.name, Model.EditState.Init),
          IO.unit
        )

      case Msg.CancelEdit =>
        model match
          case Model.Edit(id, _, p, _) => (Model.View(id, p), IO.unit)
          case m                       => (m, IO.unit)

      case Msg.SetName(t) =>
        model match
          case m: Model.Edit => (m.copy(name = t), IO.unit)
          case m             => (m, IO.unit)

      case Msg.SaveEdit =>
        model match {
          case m: Model.Edit =>
            (
              m.copy(state = Model.EditState.Updating),
              cr.send(Cmd.UpdateName(m.activityId, m.name))
            )
         case m => (m, IO.unit)
        }

      case Msg.ToView(text) =>
        (Model.View(model.activityId, text), cr.send(Cmd.SearchRefresh))

      case Msg.ToEditFailed =>
        model match {
          case m: Model.Edit =>
            (
              m.copy(state = Model.EditState.Failure),
              IO.unit
            )
         case m => (m, IO.unit)
        }
    }

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap {
      case Result.UpdateNameResult(id, name, success) =>
        model.get.flatMap {
          case m: Model.Edit if m.activityId == id =>
            if (success) model.flatModify(update(cr, Msg.ToView(name)))
            else model.flatModify(update(cr, Msg.ToEditFailed))

          case _ =>
            IO.unit
        }

      case _ =>
        IO.unit
    }

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    Resource
      .eval(subscribe(model, cr).compile.drain.start)
      .as(List(edit(model, cr), view(model, cr)).sequence)

  private def edit(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    for {
      temp <- Resource.eval(SignallingRef[IO].of(false))
      ta <- input(
        cls := "w-full min-h-max border dark:border-0 dark:bg-stone-700 px-1",
        value <-- model.map(_.name),
        onInput --> (_.evalMap(_ => temp.update(a => !a)).drain)
      )
      state = model.map {
        case m: Model.Edit => m.state
        case m: Model.View => Model.EditState.Init
      }.changes
      el <- div(
        cls <-- model
          .map(_.editMode)
          .changes
          .map(m => if (m) "flex flex-col mb-2 px-2" :: Nil else "hidden" :: Nil),
        div(
          cls := "flex flex-row mb-1",
          calico.html.io.a(
            cls := s"text-xs ${Anchor.style} mr-2",
            onClick --> ClickAction(model.flatModify(update(cr, Msg.SaveEdit))),
            disabled <-- state.map(_ == Model.EditState.Updating),
            state.map {
              case Model.EditState.Init => span("Save")
              case Model.EditState.Updating => span(i(cls := "fa fa-circle-notch animate-spin"))
              case Model.EditState.Failure => span(cls := "text-rose-500", "Updating notes failed!")
            }
          ),
          calico.html.io.a(
            cls := s"text-xs ${Anchor.style}",
            onClick --> ClickAction(model.flatModify(update(cr, Msg.CancelEdit))),
            "Cancel"
          )
        ),
        div(
          cls := "",
          ta
        )
      )
      _ <- Resource.eval(
        temp.changes.discrete
          .evalMap(_ =>
            ta.value.get.flatMap(v => model.flatModify(update(cr, Msg.SetName(v))))
          )
          .compile
          .drain
          .start
      )
    } yield el

  private def view(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    div(
      cls <-- model
        .map(_.editMode)
        .changes
        .map(m => if (!m) "flex flex-row items-center font-bold text-3xl mb-2" :: Nil else "hidden" :: Nil),
      div(
        cls := "pr-2 py-1 flex-grow text-center",
        model.map(_.name)
      ),
      calico.html.io.a(
        cls := "text-xs h-8 w-8 hover:border dark:border-stone-400 hover:bg-slate-100 hover:dark:bg-stone-700  flex flex-row items-center px-2 py-0.5 rounded",
        title := "Edit activity name",
        onClick --> ClickAction(model.flatModify(update(cr, Msg.ToEdit))),
        i(cls := "fa fa-pencil")
      )
    )
}
