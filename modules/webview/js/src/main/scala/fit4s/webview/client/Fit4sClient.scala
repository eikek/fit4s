package fit4s.webview.client

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.syntax.all.*
import fs2.io.net.Network

import fit4s.activities.data.*
import fit4s.webview.data.TagQueryParamEncoder.*
import fit4s.webview.data.{Name, Notes, RequestFailure}
import fit4s.webview.json.BasicJsonCodec.*

import com.github.eikek.borer.compats.http4s.BorerEntityJsonCodec.given
import org.http4s.*
import org.http4s.client.{Client, UnexpectedStatus}
import org.http4s.dom.FetchClientBuilder
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.headers.{Accept, MediaRangeAndQValue}

class Fit4sClient[F[_]: Async](client: Client[F], baseUrl: Uri):
  private val logger = scribe.cats.effect[F]

  def activities(
      q: Option[String],
      page: Page
  ): F[FetchResult[List[ActivityListResult]]] =
    val url =
      (baseUrl / "activity")
        .withQueryParam("limit", page.limit)
        .withQueryParam("offset", page.offset)
        .withOptionQueryParam("q", q.filter(_.nonEmpty))

    val req = Request[F](Method.GET, url)
    expectOr[List[ActivityListResult]](req).map(_.map(_.getOrElse(Nil)))

  def summary(q: Option[String]): F[FetchResult[List[ActivitySessionSummary]]] =
    val url =
      (baseUrl / "activity" / "summary").withOptionQueryParam("q", q.filter(_.nonEmpty))
    val req = Request[F](Method.GET, url)
    expectOr[List[ActivitySessionSummary]](req).map(_.map(_.getOrElse(Nil)))

  def tagList(contains: Option[String], page: Page): F[FetchResult[List[Tag]]] =
    val url = (baseUrl / "tag")
      .withOptionQueryParam("tag", contains)
      .withQueryParam("limit", page.limit)
      .withQueryParam("offset", page.offset)

    val req = Request[F](Method.GET, url)
    expectOr[List[Tag]](req).map(_.map(_.getOrElse(Nil)))

  def activityDetail(id: ActivityId): F[FetchResult[Option[ActivityDetailResult]]] =
    val url = baseUrl / "activity" / id
    val req = Request[F](Method.GET, url)
    expectOr[ActivityDetailResult](req)

  def setActivityNotes(id: ActivityId, notes: Option[String]) =
    val url = baseUrl / "activity" / id / "notes"
    notes match
      case Some(n) =>
        val req = Request[F](Method.PUT, url)
          .withEntity(Notes(n))
        client
          .successful(req)
          .attempt
          .flatMap:
            case Right(v) => v.pure[F]
            case Left(ex) => logger.error(s"Updating notes failed!", ex).as(false)

      case None =>
        val req = Request[F](Method.DELETE, url)
        client
          .successful(req)
          .attempt
          .flatMap:
            case Right(v) => v.pure[F]
            case Left(ex) => logger.error(s"Deleting notes failed!", ex).as(false)

  def setActivityName(id: ActivityId, name: String) =
    val url = baseUrl / "activity" / id / "name"
    val req = Request[F](Method.PUT, url).withEntity(Name(name))
    client
      .successful(req)
      .attempt
      .flatMap:
        case Right(v) => v.pure[F]
        case Left(ex) => logger.error(s"Updating name failed!", ex).as(false)

  def setTags(id: ActivityId, tag: Tag) =
    val url = baseUrl / "activity" / id / "tags" / tag
    val req = Request[F](Method.PUT, url)
    client
      .successful(req)
      .attempt
      .flatMap:
        case Right(v) => v.pure[F]
        case Left(ex) => logger.error(s"setting tags failed!", ex).as(false)

  def createTag(id: ActivityId, name: TagName) =
    val url = baseUrl / "activity" / id / "tags" / name
    val req = Request[F](Method.PUT, url)
    client
      .successful(req)
      .attempt
      .flatMap:
        case Right(v) => v.pure[F]
        case Left(ex) => logger.error(s"Creating tagsfailed!", ex).as(false)

  def removeTags(id: ActivityId, tag: Tag) =
    val url = baseUrl / "activity" / id / "tags" / tag
    val req = Request[F](Method.DELETE, url)
    client
      .successful(req)
      .attempt
      .flatMap:
        case Right(v) => v.pure[F]
        case Left(ex) => logger.error(s"removing tags failed!", ex).as(false)

  private def expectOr[A](
      req: Request[F]
  )(implicit d: EntityDecoder[F, A]): F[FetchResult[Option[A]]] =
    val r = if (d.consumes.nonEmpty)
      val m = d.consumes.toList
      req.addHeader(
        Accept(MediaRangeAndQValue(m.head), m.tail.map(MediaRangeAndQValue(_))*)
      )
    else req

    client
      .run(r)
      .use:
        case Status.Successful(resp) =>
          d.decode(resp, strict = false)
            .leftWiden[Throwable]
            .rethrowT
            .map(a => FetchResult.Success(a.some))
        case failedResponse =>
          failedResponse.status match
            case Status.NotFound   => FetchResult.Success(Option.empty[A]).pure[F]
            case Status.Gone       => FetchResult.Success(Option.empty[A]).pure[F]
            case Status.BadRequest =>
              EntityDecoder[F, RequestFailure]
                .decode(failedResponse, strict = false)
                .leftWiden[Throwable]
                .rethrowT
                .map(FetchResult.RequestFailed.apply)
            case _ =>
              Async[F]
                .raiseError(UnexpectedStatus(failedResponse.status, req.method, req.uri))

object Fit4sClient:
  def apply[F[_]: Network: Async](
      baseUrl: Uri,
      httpTimeout: Duration
  ): Resource[F, Fit4sClient[F]] =
    EmberClientBuilder
      .default[F]
      .withTimeout(httpTimeout)
      .withIdleConnectionTime(httpTimeout * 2)
      .build
      .map(new Fit4sClient[F](_, baseUrl))

  def create[F[_]: Async](baseUrl: Uri) =
    new Fit4sClient[F](FetchClientBuilder[F].create, baseUrl)
