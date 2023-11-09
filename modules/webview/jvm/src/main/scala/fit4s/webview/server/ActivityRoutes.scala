package fit4s.webview.server

import java.time.ZoneId

import cats.data.NonEmptyList as Nel
import cats.effect.*
import cats.syntax.all.*

import fit4s.activities.ActivityLog
import fit4s.activities.data.QueryCondition
import fit4s.http.borer.BorerEntityCodec.Implicits.*
import fit4s.webview.data.*
import fit4s.webview.json.BasicJsonCodec
import fit4s.webview.server.util.*

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class ActivityRoutes[F[_]: Async](log: ActivityLog[F], zoneId: ZoneId)
    extends Http4sDsl[F]
    with BasicJsonCodec
    with MoreHttp4sDsl[F] {

  def routes: HttpRoutes[F] = HttpRoutes.of {
    case GET -> Root :? ActivityQueryVar(query) =>
      for {
        now <- Clock[F].realTimeInstant
        input = query(zoneId, now)
        resp <- input.map { q =>
          log
            .activityList(q)
            .compile
            .toVector
            .flatMap(Ok(_))
        }.orBadRequest
      } yield resp

    case GET -> Root / "summary" :? ActivitySummaryQueryVar(query) =>
      for {
        now <- Clock[F].realTimeInstant
        input = query(zoneId, now)
        resp <- input.map { q =>
          log
            .activitySummary(q)
            .flatMap(Ok(_))
        }.orBadRequest
      } yield resp

    case GET -> Root / ActivityIdVar(id) =>
      for {
        act <- log.activityDetails(id, true)
        resp <- act.orNotFound(s"$id not found")
      } yield resp

    case req @ PUT -> Root / ActivityIdVar(id) / "name" =>
      for {
        input <- req.as[Name]
        _ <- log.setActivityName(id, input.name)
        resp <- Ok()
      } yield resp

    case DELETE -> Root / ActivityIdVar(id) / "name" =>
      log.setGeneratedActivityName(id, zoneId) *> Ok()

    case req @ PUT -> Root / ActivityIdVar(id) / "notes" =>
      for {
        input <- req.as[Notes]
        _ <- log.setActivityNotes(id, input.notes.some)
        resp <- Ok()
      } yield resp

    case DELETE -> Root / ActivityIdVar(id) / "notes" =>
      log.setActivityNotes(id, None) *> Ok()

    case PUT -> Root / ActivityIdVar(id) / "tags" / TagNamesVar(tag) =>
      log.tagRepository.linkTags(QueryCondition(id).some, Nel.of(tag)) *> Ok()

    case DELETE -> Root / ActivityIdVar(id) / "tags" / TagNamesVar(tag) =>
      log.tagRepository.unlinkTags(QueryCondition(id).some, Nel.of(tag)) *> Ok()
  }
}
