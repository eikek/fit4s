package fit4s.activities.data

final case class Location(id: LocationId, location: String) extends LocationPlatform

object Location extends LocationCompanion
