package fit4s.webview.server

import java.time.ZoneId

import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.ActivityLog
import fit4s.http.borer.BorerEntityCodec.Implicits.*
import fit4s.webview.data.{TagAndQuery, TagRename}
import fit4s.webview.json.BasicJsonCodec
import fit4s.webview.server.util.*

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class TagRoutes[F[_]: Async](log: ActivityLog[F], zoneId: ZoneId)
    extends Http4sDsl[F]
    with BasicJsonCodec
    with MoreHttp4sDsl[F]:

  def routes: HttpRoutes[F] = HttpRoutes.of:
    case GET -> Root :? PageVar(page) +& TagNamesVar(tags) =>
      (page, tags.map(_.headOption)).mapN { (p, contains) =>
        log.tagRepository
          .listTags(contains, p)
          .compile
          .toList
          .flatMap(Ok(_))
      }.orBadRequest

    case DELETE -> Root / TagIdVar(id) =>
      log.tagRepository
        .removeById(id)
        .flatMap(n => if (n > 0) Ok() else NotFoundF(s"$id not found"))

    case req @ PUT -> Root / "link" =>
      for {
        input <- req.as[TagAndQuery]

        currentTime <- Clock[F].realTimeInstant

        resp <-
          input.query
            .traverse(QueryConditionVar.validate(zoneId, currentTime))
            .map { cond =>
              log.tagRepository.linkTags(cond, input.tags) *> Created()
            }
            .orBadRequest
      } yield resp

    case req @ PUT -> Root / "unlink" =>
      for {
        input <- req.as[TagAndQuery]

        currentTime <- Clock[F].realTimeInstant

        resp <-
          input.query
            .traverse(QueryConditionVar.validate(zoneId, currentTime))
            .map { cond =>
              log.tagRepository.unlinkTags(cond, input.tags) *> Created()
            }
            .orBadRequest
      } yield resp

    case req @ PUT -> Root / "rename" =>
      for {
        input <- req.as[TagRename]
        result <- log.tagRepository.rename(input.from, input.to)
        resp <- if (result) Created() else Ok()
      } yield resp
