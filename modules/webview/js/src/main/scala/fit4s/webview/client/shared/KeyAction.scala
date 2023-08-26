package fit4s.webview.client.shared

import cats.FlatMap
import cats.effect.Concurrent
import cats.syntax.all.*
import fs2.dom.KeyboardEvent
import fs2.{Pipe, Stream}

object KeyAction {

  def apply[F[_]: FlatMap: Concurrent](action: String => F[Unit]): Pipe[F, KeyboardEvent[F], Nothing] =
    _.switchMap(ev => Stream.exec(action(ev.key) *> ev.preventDefault))

  def onEnter[F[_]: FlatMap: Concurrent](action: F[Unit]): Pipe[F, KeyboardEvent[F], Nothing] =
    apply(key => if ("enter".equalsIgnoreCase(key)) action else ().pure[F])
}
