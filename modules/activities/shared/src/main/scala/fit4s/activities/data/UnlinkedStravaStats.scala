package fit4s.activities.data

import java.time.Instant

final case class UnlinkedStravaStats(
    lowestStart: Instant,
    recentStart: Instant,
    count: Int
)
