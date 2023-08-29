package fit4s.webview.client.cmd

import cats.data.NonEmptyList

import fit4s.activities.data.*

enum Cmd:
  case SearchCmd(query: String, page: Page)
  case SearchListOnlyCmd(query: String, page: Page)
  case SearchRefresh
  case GetBikeTags
  case GetTags(filter: String)
  case SearchTagSummary(query: String, tags: List[Tag])
  case SetDetailPage(id: ActivityId)
  case SetSearchPage
  case SetSearchQuery(query: String)
  case UpdateNotes(id: ActivityId, notes: String)
  case UpdateName(id: ActivityId, name: String)
  case SetTag(id: ActivityId, tag: Tag)
  case RemoveTag(id: ActivityId, tag: Tag)
  case CreateTag(id: ActivityId, name: TagName)
