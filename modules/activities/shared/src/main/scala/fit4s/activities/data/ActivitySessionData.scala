package fit4s.activities.data

import java.time.Instant

import cats.syntax.all.*

import fit4s.data.*

final case class ActivitySessionData(
    id: ActivitySessionDataId,
    activitySessionId: ActivitySessionId,
    timestamp: Instant,
    position: Option[Position],
    altitude: Option[Distance],
    heartRate: Option[HeartRate],
    cadence: Option[Cadence],
    distance: Option[Distance],
    speed: Option[Speed],
    power: Option[Power],
    grade: Option[Grade],
    temperature: Option[Temperature],
    calories: Option[Calories]
) {

  def pair[A, B, C](
      fa: ActivitySessionData => Option[A],
      fb: ActivitySessionData => Option[B]
  )(g: (A, B) => C): Option[C] =
    (fa(this), fb(this)).mapN(g)
}
