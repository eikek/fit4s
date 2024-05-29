package fit4s.webview.client.detail

import java.time.ZoneId

import cats.Eq
import cats.effect.{IO, Resource}
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import fit4s.activities.data.ActivityDetailResult
import fit4s.profile.types.Sport
import fit4s.webview.client.cmd.CommandRuntime
import fit4s.webview.client.shared.*
import fit4s.webview.client.util.*

import calico.html.io.{*, given}

object ActivityOverviewPanel:

  def render(
      model: SignallingRef[IO, DetailPage.Model],
      cr: CommandRuntime[IO],
      details: ActivityDetailResult,
      zone: ZoneId
  ) =
    div(
      cls := "flex flex-col flex-grow",
      title(model, cr, details, zone),
      TagEdit.render(
        SignallingRef.lens(model)(_.tagEdit, a => b => a.copy(tagEdit = b)),
        cr
      ),
      NotesEdit
        .render(
          SignallingRef.lens(model)(_.notesEdit, a => b => a.copy(notesEdit = b)),
          cr
        )
    )

  private def title(
      model: SignallingRef[IO, DetailPage.Model],
      cr: CommandRuntime[IO],
      details: ActivityDetailResult,
      zone: ZoneId
  ) =
    div(
      cls := "flex flex-row items-center",
      div(
        cls := "px-2 py-2 flex flex-row items-center",
        model
          .map(_.details.sessions.map(_.sport))
          .changes
          .map(m => SportIcon.of(m, "fa-4x mx-auto" :: Nil))
      ),
      div(
        cls := "flex flex-col flex-grow",
        div(
          cls := "opacity-75 mb-2 text-sm text-center",
          FormatTimestamp(
            details.activity.created.getOrElse(details.activity.timestamp),
            zone
          ),
          startEndLabel(details)
        ),
        NameEdit.render(
          SignallingRef.lens(model)(_.nameEdit, a => b => a.copy(nameEdit = b)),
          cr
        )
      )
    )

  private def startEndLabel(details: ActivityDetailResult) =
    val start = details.startPlace.get(details.sessions.head.id).map(_.location)
    val finish = details.endPlace.get(details.sessions.head.id).map(_.location)
    (start, finish) match
      case (Some(s), Some(f)) if s != f => s", from $s to $f"
      case (Some(s), _)                 => s", in $s"
      case (_, Some(f))                 => s", to $f"
      case (_, _)                       => ""

  given Eq[Sport] = Eq.fromUniversalEquals
