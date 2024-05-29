package fit4s.activities.data

import fs2.io.file.Path

trait LocationPlatform:

  val location: String

  val locationPath: Path = Path(location)

trait LocationCompanion:
  def apply(id: LocationId, path: Path): Location =
    Location(id, path.absolute.toString)
