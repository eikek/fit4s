package fit4s.activities.impl

import cats.syntax.all._
import doobie._
import doobie.implicits._
import fit4s.activities.records.{RActivity, RActivityLocation}

import java.time.Instant

case class SyncData(locations: Vector[RActivityLocation], lastImport: Instant) {
  def isEmpty: Boolean = locations.isEmpty
}

object SyncData {

  val empty: SyncData = SyncData(Vector.empty, Instant.MIN)

  def get: ConnectionIO[SyncData] =
    (
      RActivityLocation.listAll,
      RActivity.latestImport.map(_.getOrElse(Instant.MIN))
    )
      .mapN(SyncData.apply)
}
