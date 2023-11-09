package fit4s.webview.client.detail

import java.time.ZoneId

import cats.Eq
import cats.effect.{IO, Resource}
import fs2.concurrent.SignallingRef

import fit4s.activities.data.ActivityDetailResult
import fit4s.webview.client.cmd.*
import fit4s.webview.client.shared.*

import calico.html.io.{*, given}

object DetailPage {

  final case class Model(
      details: ActivityDetailResult,
      notesEdit: NotesEdit.Model,
      nameEdit: NameEdit.Model,
      tagEdit: TagEdit.Model
  )
  object Model:
    def make(details: ActivityDetailResult): Model =
      Model(
        details,
        NotesEdit.Model.of(details.activity.id, details.activity.notes),
        NameEdit.Model.of(details.activity.id, details.activity.name),
        TagEdit.Model(details.activity.id, details.tags)
      )
    given Eq[Model] = Eq.fromUniversalEquals

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO], zone: ZoneId) =
    Resource.eval(model.map(_.details).get).flatMap { details =>
      div(
        cls := "flex flex-col",
        div(
          cls := "flex my-2",
          a(
            cls := Anchor.style,
            onClick --> ClickAction(cr.send(Cmd.SetSearchPage)),
            "Back"
          )
        ),

        // main panel: left header + notes, right data
        div(
          cls := "flex flex-row space-x-1 border-b dark:border-stone-700 rounded",

          // header: left sport icon, right content
          ActivityOverviewPanel.render(model, cr, details, zone),

          // data
          DataOverviewPanel(details)
        ),

        // map panel container
        MapPanel(details),

        // Laps
        div(cls := "text-2xl font-bold mt-3 mb-1", "Laps"),
        LapPanel(details, zone)
      )
    }
}
