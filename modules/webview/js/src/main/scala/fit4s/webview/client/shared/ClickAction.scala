package fit4s.webview.client.shared

import cats.FlatMap
import cats.effect.Concurrent
import cats.syntax.all.*
import fs2.dom.MouseEvent
import fs2.{Pipe, Stream}

object ClickAction {

  def apply[F[_]: FlatMap: Concurrent](action: F[Unit]): Pipe[F, MouseEvent[F], Nothing] =
    _.switchMap(ev => Stream.exec(action *> ev.preventDefault))
}
