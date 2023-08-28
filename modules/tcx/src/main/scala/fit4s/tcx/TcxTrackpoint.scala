package fit4s.tcx

import java.time.Instant

import fit4s.data.*

final case class TcxTrackpoint(
    time: Instant,
    position: Option[Position],
    altitude: Option[Distance],
    distance: Option[Distance],
    heartRate: Option[HeartRate],
    cadence: Option[Cadence]
)
