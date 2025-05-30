package fit4s.strava.impl

import java.net.ServerSocket

import scala.concurrent.duration.*

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*
import fs2.io.net.Network

import fit4s.strava.data.*
import fit4s.strava.{StravaAppCredentials, StravaClientConfig}

import com.comcast.ip4s.{Host, Port}
import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec.given
import org.http4s.Method.POST
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpRoutes, Response, UrlForm}
import scodec.bits.ByteVector

final class StravaOAuth[F[_]: Async: Network](
    config: StravaClientConfig,
    client: Client[F]
):

  private val logger = scribe.cats[F]

  def init(cfg: StravaAppCredentials, timeout: FiniteDuration): F[Option[TokenAndScope]] =
    for {
      whenDone <- Deferred[F, Option[TokenAndScope]]
      _ <- Async[F].start(
        Async[F]
          .timeoutTo(whenDone.get.as(true), timeout, whenDone.complete(None))
      )

      state <- Random.scalaUtilRandom
        .flatMap(_.nextBytes(15))
        .map(a => ByteVector.view(a).toBase58)

      uriAndServer <- createServer(cfg, state, whenDone)
      (uri, server) = uriAndServer
      _ <-
        server.use { _ =>
          val authRequestUri =
            config.authUrl
              .withQueryParam("client_id", cfg.clientId)
              .withQueryParam("response_type", "code")
              .withQueryParam("redirect_uri", uri)
              .withQueryParam("state", state)
              .withQueryParam("scope", StravaScope.activityReadAndWrite.asString)

          println(s"\nOpen in a browser to complete:\n${authRequestUri.renderString}")
            .flatMap(_ =>
              whenDone.get
                .flatMap {
                  case Some(_) =>
                    println("Authorization successful!")
                  case None =>
                    println("Authorization was not successful!")
                }
            )
        }
      token <- whenDone.get
    } yield token

  def refresh(
      cfg: StravaAppCredentials,
      latestToken: F[TokenAndScope]
  ): F[TokenAndScope] =
    for {
      latestDbToken <- latestToken
      _ <- logger.debug("Refresh latest token")
      refreshed <- tokenRefresh(cfg, latestDbToken.tokenResponse.refresh_token)
    } yield TokenAndScope(refreshed, latestDbToken.scope)

  private def findPort: F[Port] =
    Resource
      .make(Async[F].blocking(new ServerSocket(0)))(ss => Async[F].blocking(ss.close()))
      .use(s => Async[F].blocking(s.getLocalPort).map(Port.fromInt))
      .flatMap:
        case Some(p) => p.pure[F]
        case None    => findPort

  private def createServer(
      cfg: StravaAppCredentials,
      state: String,
      whenDone: Deferred[F, Option[TokenAndScope]]
  ) =
    for {
      port <- findPort
      host <- Host
        .fromString("localhost")
        .map(_.pure[F])
        .getOrElse(Async[F].raiseError(new Exception("invalid host")))

      server = EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withShutdownTimeout(1.second)
        .withHttpApp(tokenRoute(cfg, state, whenDone).orNotFound)
        .build
        .onFinalize(logger.debug("Shut down strava token receive service"))

      uri = show"http://$host:$port/fit4s/strava"
    } yield (uri, server)

  private def tokenRoute(
      cfg: StravaAppCredentials,
      state: String,
      whenDone: Deferred[F, Option[TokenAndScope]]
  ): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val none = Option.empty[TokenAndScope]

    HttpRoutes.of { case req @ GET -> Root / "fit4s" / "strava" =>
      val resp: F[(Response[F], Option[TokenAndScope])] =
        req.params.get("error") match
          case Some(err) =>
            logger.error(s"Strava responded with an error: $err") *>
              Forbidden(s"Strava disallowed access: $err").map(_ -> none)

          case None =>
            logger.debug(s"Strava responded successful: ${req.params}") *>
              (req.params.get("code"), req.params.get("scope"), req.params.get("state"))
                .mapN { (code, scope, receivedState) =>
                  if (state != receivedState)
                    Forbidden(
                      s"The request did not deliver the correct state from the process initiation"
                    ).map(_ -> none)
                  else
                    tokenExchange(code, cfg)
                      .map(r => TokenAndScope(r, StravaScope(scope)))
                      .attempt
                      .flatMap:
                        case Right(r) =>
                          Ok(r).map(_ -> r.some)

                        case Left(ex) =>
                          logger.error("Error in token exchange", ex) *>
                            InternalServerError(s"Error in token exchange!").map(
                              _ -> none
                            )
                }
                .getOrElse(
                  Forbidden(s"Strava did not respond with an access code").map(_ -> none)
                )

      resp.flatMap { case (resp, token) =>
        logger.debug("Authorize flow done.") *> whenDone.complete(token).as(resp)
      }
    }

  def tokenExchange(code: String, cfg: StravaAppCredentials) =
    val dsl = Http4sClientDsl[F]
    import dsl._

    val req =
      POST(config.tokenUri)
        .withEntity(
          UrlForm(
            "client_id" -> cfg.clientId,
            "client_secret" -> cfg.clientSecret,
            "code" -> code,
            "grant_type" -> "authorization_code"
          )
        )

    logger.debug(s"Issue token exchange request to ${config.tokenUri}") *>
      client
        .expect[StravaTokenResponse](req)
        .attempt
        .flatTap:
          case Left(ex) => logger.error("Error in token exchange", ex)
          case Right(r) => logger.debug(s"Got token response: $r")
        .rethrow

  def tokenRefresh(cfg: StravaAppCredentials, refreshToken: StravaRefreshToken) =
    val dsl = Http4sClientDsl[F]
    import dsl._

    val req =
      POST(config.tokenUri)
        .withEntity(
          UrlForm(
            "client_id" -> cfg.clientId,
            "client_secret" -> cfg.clientSecret,
            "refresh_token" -> refreshToken.token,
            "grant_type" -> "refresh_token"
          )
        )

    logger.debug(s"Issue refresh token request to ${config.tokenUri}") *>
      client
        .expect[StravaTokenResponse](req)
        .attempt
        .flatTap:
          case Left(ex) => logger.error("Error in token refresh", ex)
          case Right(r) => logger.debug(s"Got token response: $r")
        .rethrow

  private def println(m: String): F[Unit] =
    Async[F].blocking(Predef.println(m))
