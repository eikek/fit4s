package fit4s.webview.client.cmd

import cats.Eq

import fit4s.activities.data.*
import fit4s.webview.client.{FetchResult, Fit4sClient}

enum Result:
  case SearchResult(
      cmd: Cmd.SearchCmd,
      activities: FetchResult[(List[ActivityListResult], List[ActivitySessionSummary])]
  )
  case SearchListOnlyResult(
      cmd: Cmd.SearchListOnlyCmd,
      activites: FetchResult[List[ActivityListResult]]
  )
  case GetBikeTagsResult(tags: FetchResult[List[Tag]])
  case GetTagsResult(tags: FetchResult[List[Tag]])
  case TagSummaryResult(result: FetchResult[List[(Tag, List[ActivitySessionSummary])]])
  case DetailResult(result: FetchResult[Option[ActivityDetailResult]])
  case SetSearchPageResult
  case SetSearchQueryResult(q: String)
  case UpdateNotesResult(id: ActivityId, notes: String, success: Boolean)
  case UpdateNameResult(id: ActivityId, notes: String, success: Boolean)
  case SetTagsResult(id: ActivityId, tag: Tag, success: Boolean)
  case RemoveTagsResult(id: ActivityId, tag: Tag, success: Boolean)
  case CreateTagResult(id: ActivityId, name: TagName, success: Boolean)
  case SearchRefresh

object Result:
  given Eq[Result] = Eq.fromUniversalEquals
