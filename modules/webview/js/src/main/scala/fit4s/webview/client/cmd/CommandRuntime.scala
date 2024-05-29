package fit4s.webview.client.cmd

import cats.Parallel
import cats.effect.*
import cats.syntax.all.*
import fs2.Stream
import fs2.concurrent.Topic

import fit4s.activities.data.*
import fit4s.webview.client.{FetchResult, Fit4sClient}

trait CommandRuntime[F[_]]:
  def send(cmd: Cmd): F[Unit]

  def subscribe: Stream[F, Result]

object CommandRuntime:
  def apply[F[_]: Async: Parallel](
      fit4sClient: Fit4sClient[F]
  ): F[CommandRuntime[F]] =
    Topic[F, Result].map(t => new Impl[F](t, fit4sClient))

  final private class Impl[F[_]: Async: Parallel](
      topic: Topic[F, Result],
      fit4sClient: Fit4sClient[F]
  ) extends CommandRuntime[F]:
    private val logger = scribe.cats.effect[F]
    override def subscribe: Stream[F, Result] = topic.subscribeUnbounded.changes

    override def send(cmd: Cmd): F[Unit] =
      cmd match
        case c @ Cmd.SearchCmd(q, page) =>
          val query = q.some.map(_.trim).filter(_.nonEmpty)
          (fit4sClient.activities(query, page), fit4sClient.summary(query))
            .parMapN { (list, sum) =>
              val r = (list, sum).mapN(_ -> _)
              Result.SearchResult(c, r)
            }
            .flatMap(topic.publish1)
            .void

        case c @ Cmd.SearchListOnlyCmd(q, page) =>
          val query = q.some.map(_.trim).filter(_.nonEmpty)
          fit4sClient
            .activities(query, page)
            .map(Result.SearchListOnlyResult(c, _))
            .flatMap(topic.publish1)
            .void

        case Cmd.GetTags(filter) =>
          val query = filter.some.map(_.trim).filter(_.nonEmpty)
          fit4sClient
            .tagList(query, Page.one(200))
            .map(Result.GetTagsResult.apply)
            .flatMap(topic.publish1)
            .void

        case Cmd.GetBikeTags =>
          fit4sClient
            .tagList("Bike/".some, Page.one(20))
            .map(Result.GetBikeTagsResult.apply)
            .flatMap(topic.publish1)
            .void

        case Cmd.GetShoeTags =>
          fit4sClient
            .tagList("Shoe/".some, Page.one(20))
            .map(Result.GetShoeTagsResult.apply)
            .flatMap(topic.publish1)
            .void

        case req @ Cmd.SearchTagSummary(query, tags) =>
          if (tags.isEmpty) logger.info(s"No tags provided for search-tags-summary")
          else
            val results: F[List[(Tag, FetchResult[List[ActivitySessionSummary]])]] =
              tags
                .map(t => t -> s"""$query tag="${t.name.name}"""")
                .parTraverse:
                  case (t, q) =>
                    fit4sClient.summary(q.some).map(r => t -> r)
            results
              .map(_.traverse { case (t, fr) => fr.map(r => t -> r) })
              .map(Result.TagSummaryResult(req, _))
              .flatMap(topic.publish1)
              .void

        case Cmd.SetDetailPage(id) =>
          fit4sClient
            .activityDetail(id)
            .map(Result.DetailResult.apply)
            .flatMap(topic.publish1)
            .void

        case Cmd.SetSearchPage =>
          topic.publish1(Result.SetSearchPageResult).void

        case Cmd.SetSearchQuery(q) =>
          topic.publish1(Result.SetSearchQueryResult(q)).void

        case Cmd.UpdateNotes(id, notes) =>
          val n = Option(notes).map(_.trim).filter(_.nonEmpty)
          fit4sClient
            .setActivityNotes(id, n)
            .map(Result.UpdateNotesResult(id, notes, _))
            .flatMap(topic.publish1)
            .void

        case Cmd.UpdateName(id, name) =>
          fit4sClient
            .setActivityName(id, name)
            .map(Result.UpdateNameResult(id, name, _))
            .flatMap(topic.publish1)
            .void

        case Cmd.SetTag(id, tag) =>
          fit4sClient
            .setTags(id, tag)
            .map(Result.SetTagsResult(id, tag, _))
            .flatMap(topic.publish1)
            .void

        case Cmd.CreateTag(id, name) =>
          fit4sClient
            .createTag(id, name)
            .map(Result.CreateTagResult(id, name, _))
            .flatMap(topic.publish1)
            .void

        case Cmd.RemoveTag(id, tag) =>
          fit4sClient
            .removeTags(id, tag)
            .map(Result.RemoveTagsResult(id, tag, _))
            .flatMap(topic.publish1)
            .void

        case Cmd.SearchRefresh =>
          topic.publish1(Result.SearchRefresh).void
