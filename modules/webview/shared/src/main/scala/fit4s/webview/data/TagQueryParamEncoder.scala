package fit4s.webview.data

import fit4s.activities.data.{Tag, TagName}

import org.http4s.QueryParamEncoder
import org.http4s.Uri

object TagQueryParamEncoder:

  implicit val tagQueryParamEncoder: QueryParamEncoder[Tag] =
    QueryParamEncoder.stringQueryParamEncoder.contramap(_.name.name)

  implicit val tagSegmentEncoder: Uri.Path.SegmentEncoder[Tag] =
    Uri.Path.SegmentEncoder.stringSegmentEncoder.contramap(_.name.name)

  implicit val tagNameSegmentEncoder: Uri.Path.SegmentEncoder[TagName] =
    Uri.Path.SegmentEncoder.stringSegmentEncoder.contramap(_.name)
