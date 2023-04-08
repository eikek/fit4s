package fit4s.activities

import fit4s.activities.data.{ActivityId, TagName}
import fs2.io.file.Path
import fs2.Stream

trait StravaSupport[F[_]] {

  def loadExport(
      stravaExport: Path,
      tagged: Set[TagName],
      bikeTagPrefix: Option[TagName],
      shoeTagPrefix: Option[TagName],
      commuteTag: Option[TagName],
      callback: ImportCallback[F],
      concN: Int
  ): Stream[F, ImportResult[ActivityId]]
}
