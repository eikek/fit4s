package fit4s.webview.client.shared

import cats.Eq
import cats.effect.*
import fs2.concurrent.SignallingRef
import fs2.dom.*

import calico.html.io.{*, given}

object TextField:
  val inputStyle =
    "px-2 py-2 flex-grow border dark:border-0 dark:bg-stone-700 rounded"

  final case class Config(
      placeholder: String,
      cls: String,
      typ: String,
      onEnter: IO[Unit]
  )
  object Config:
    val default: Config = Config("Search...", inputStyle, "search", IO.unit)

  final case class Model(text: String)
  object Model:
    val empty: Model = Model("")
    def makeEmpty: IO[SignallingRef[IO, Model]] = SignallingRef[IO].of(empty)
    given Eq[Model] = Eq.fromUniversalEquals

  enum Msg:
    case SetText(str: String)

  def update(msg: Msg)(model: Model): Model =
    msg match
      case Msg.SetText(str) =>
        model.copy(text = str)

  def render(model: SignallingRef[IO, Model], cfg: Config) =
    for {
      temp <- Resource.eval(SignallingRef[IO].of(false))
      el <- input(
        cls := cfg.cls,
        typ := cfg.typ,
        placeholder := cfg.placeholder,
        value <-- model.map(_.text).changes,
        onInput --> (_.evalMap(_ => temp.update(a => !a)).drain),
        onKeyUp --> KeyAction.onEnter(cfg.onEnter)
      )
      _ <- Resource.eval(
        temp.changes.discrete
          .evalMap(_ => el.value.get.flatMap(v => model.update(update(Msg.SetText(v)))))
          .compile
          .drain
          .start
      )
    } yield el
