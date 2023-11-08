package fit4s.webview.client.search

import cats.effect.*
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import fs2.dom.HtmlDivElement

import fit4s.webview.client.shared.ClickAction
import fit4s.webview.client.shared.TextField

import calico.html.io.{*, given}

object SearchField {
  val containerStyle =
    "search py-4 flex flex-row items-center dark:border-stone-600 rounded-md"
  val buttonStyle =
    "px-4 py-2 ml-2 rounded border dark:border-stone-500 hover:bg-sky-500 hover:border-sky-400 hover:text-stone-800"

  final case class Model(
      busy: Boolean,
      text: TextField.Model
  ) {
    def setText(str: String): Model = copy(text = text.copy(text = str))
    def setBusy: Model = copy(busy = true)
    def unsetBusy: Model = copy(busy = false)
  }
  object Model:
    val empty: Model = Model(false, TextField.Model.empty)
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)

  def apply[F[_]](
      model: SignallingRef[IO, Model],
      on: String => IO[Unit]
  ): Resource[IO, HtmlDivElement[IO]] = {
    val textModel = SignallingRef.lens[IO, Model, TextField.Model](model)(
      get = _.text,
      set = a => b => a.copy(text = b)
    )
    val searchAction = model.get.map(_.text.text).flatMap(on)
    val queryInputR =
      TextField.render(textModel, TextField.Config.default.copy(onEnter = searchAction))
    val btn = button(
      typ := "submit",
      onClick --> ClickAction(searchAction),
      i(cls <-- model.map(_.busy).changes.map {
        case true  => "fa fa-circle-notch animate-spin" :: Nil
        case false => "fa fa-search" :: Nil
      }),
      cls := buttonStyle,
      disabled <-- model.map(_.busy).changes
    )

    div(
      cls := containerStyle,
      queryInputR,
      btn
    )
  }
}
