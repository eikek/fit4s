package fit4s.activities.records

import fs2.io.file.Path

final case class ActivityLocationRecord(id: Long, location: Path)
