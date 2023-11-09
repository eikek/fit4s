package fit4s.activities.data

import java.time.{Duration, Instant}

import fit4s.data.FileId

final case class Activity(
    id: ActivityId,
    locationId: LocationId,
    path: String,
    activityFileId: FileId,
    device: DeviceInfo,
    serialNumber: Option[Long],
    created: Option[Instant],
    name: String,
    timestamp: Instant,
    totalTime: Duration,
    notes: Option[String],
    importDate: Instant
)

object Activity {
  def apply(
      id: ActivityId,
      locationId: LocationId,
      path: String,
      activityFileId: FileId,
      name: String,
      timestamp: Instant,
      totalTime: Duration,
      notes: Option[String],
      importDate: Instant
  ): Activity = Activity(
    id,
    locationId,
    path,
    activityFileId,
    DeviceInfo.Product(activityFileId.product),
    activityFileId.serialNumber,
    activityFileId.createdAt.map(_.asInstant),
    name,
    timestamp,
    totalTime,
    notes,
    importDate
  )
}
