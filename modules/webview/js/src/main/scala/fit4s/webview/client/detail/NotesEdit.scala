package fit4s.webview.client.shared

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import fit4s.activities.data.ActivityId
import fit4s.webview.client.cmd.*

import calico.html.io.{*, given}

object NotesEdit {

  sealed trait Model {
    def activityId: ActivityId
    def text: String
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
    case class View(activityId: ActivityId, text: String) extends Model {
      val editMode = false
    }
    case class Edit(activityId: ActivityId, text: String, prev: String, state: EditState)
        extends Model {
      val editMode = true
    }
    def of(activityId: ActivityId, text: Option[String]): Model =
      View(activityId, text.getOrElse(""))

  enum Msg:
    case ToEdit
    case CancelEdit
    case SaveEdit
    case SetText(text: String)
    case ToView(text: String)
    case ToEditFailed

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match {
      case Msg.ToEdit =>
        (
          Model.Edit(model.activityId, model.text, model.text, Model.EditState.Init),
          IO.unit
        )

      case Msg.CancelEdit =>
        model match
          case Model.Edit(id, _, p, _) => (Model.View(id, p), IO.unit)
          case m                       => (m, IO.unit)

      case Msg.SetText(t) =>
        model match
          case m: Model.Edit => (m.copy(text = t), IO.unit)
          case m             => (m, IO.unit)

      case Msg.SaveEdit =>
        model match {
          case m: Model.Edit =>
            (
              m.copy(state = Model.EditState.Updating),
              cr.send(Cmd.UpdateNotes(m.activityId, m.text))
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
      case Result.UpdateNotesResult(id, text, success) =>
        model.get.flatMap {
          case m: Model.Edit if m.activityId == id =>
            if (success) model.flatModify(update(cr, Msg.ToView(text)))
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
      ta <- textArea(
        cls := "w-full min-h-max border dark:border-0 dark:bg-stone-700 px-1",
        value <-- model.map(_.text),
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
          .map(m => if (m) "flex flex-col mt-2 py-2" :: Nil else "hidden" :: Nil),
        div(
          cls := "flex flex-row mb-1",
          calico.html.io.a(
            cls := s"text-xs ${Anchor.style} mr-2",
            onClick --> ClickAction(model.flatModify(update(cr, Msg.SaveEdit))),
            disabled <-- state.map(_ == Model.EditState.Updating),
            state.map {
              case Model.EditState.Init => span("Save")
              case Model.EditState.Updating =>
                span(i(cls := "fa fa-circle-notch animate-spin"))
              case Model.EditState.Failure =>
                span(cls := "text-rose-500", "Updating notes failed!")
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
            ta.value.get.flatMap(v => model.flatModify(update(cr, Msg.SetText(v))))
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
        .map(m => if (!m) "flex flex-col mt-2 py-2 text-md" :: Nil else "hidden" :: Nil),
      div(
        cls := "flex flex-row",
        calico.html.io.a(
          cls := s"text-xs ${Anchor.style}",
          onClick --> ClickAction(model.flatModify(update(cr, Msg.ToEdit))),
          "Edit notes"
        )
      ),
      div(
        cls := "px-2 py-1",
        model.map(_.text)
      )
    )
}
