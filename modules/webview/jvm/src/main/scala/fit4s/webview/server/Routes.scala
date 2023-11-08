package fit4s.webview.server

import java.time.ZoneId

import cats.effect.*

import fit4s.activities.ActivityLog

import org.http4s.HttpRoutes
import org.http4s.server.Router

final class Routes[F[_]: Async](log: ActivityLog[F], zoneId: ZoneId) {

  private[this] val tag = new TagRoutes[F](log, zoneId)
  private[this] val activity = new ActivityRoutes[F](log, zoneId)
  private[this] val ui = new UiRoutes[F]

  val all: HttpRoutes[F] =
    Router(
      "api/activity" -> activity.routes,
      "api/tag" -> tag.routes,
      "" -> ui.routes
    )
}
