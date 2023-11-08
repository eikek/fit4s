package fit4s.webview.server

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network

import fit4s.activities.ActivityLog
import fit4s.webview.server.util.ErrorResponse

import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, Logger}
import org.http4s.{HttpRoutes, Response}

object Fit4sServer {

  def httpRoutes[F[_]: Async](log: ActivityLog[F], zoneId: ZoneId): HttpRoutes[F] = {
    val cors = CORS.policy
    val logger = scribe.cats.effect[F]
    Logger.httpRoutes(
      logHeaders = true,
      logBody = false,
      logAction = Some(msg => logger.info(msg))
    )(
      cors(new Routes[F](log, zoneId).all)
    )
  }

  def apply[F[_]: Async: Network](
      host: Host,
      port: Port,
      log: ActivityLog[F],
      zoneId: ZoneId
  ): Resource[F, Server] = {
    val logger = scribe.cats.effect[F]
    EmberServerBuilder
      .default[F]
      .withHost(host)
      .withPort(port)
      .withHttpApp(httpRoutes(log, zoneId).orNotFound)
      .withErrorHandler { case ex =>
        logger
          .error("Service raised an error!", ex)
          .as(ErrorResponse(ex))
      }
      .build
  }
}
