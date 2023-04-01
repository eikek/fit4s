package fit4s.activities

import fit4s.activities.data.Page
import fit4s.activities.records.ActivityLocationRecord
import fs2.io.file.Path
import fs2.Stream

trait LocationRepo[F[_]] {
  def createLocation(name: Path): F[ImportResult[ActivityLocationRecord]]

  def deleteLocation(id: Path): F[Int]

  def updateLocation(location: ActivityLocationRecord): F[Int]

  def listLocations(
      contains: Option[String],
      page: Page
  ): Stream[F, ActivityLocationRecord]
}
