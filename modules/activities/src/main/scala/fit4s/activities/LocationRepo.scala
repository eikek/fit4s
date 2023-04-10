package fit4s.activities

import fit4s.activities.data.Page
import fit4s.activities.records.RActivityLocation
import fs2.io.file.Path
import fs2.Stream

trait LocationRepo[F[_]] {
  def createLocation(name: Path): F[ImportResult[RActivityLocation]]

  def deleteLocation(id: Path): F[Int]

  def updateLocation(location: RActivityLocation): F[Int]

  def listLocations(
      contains: Option[String],
      page: Page
  ): Stream[F, RActivityLocation]
}
