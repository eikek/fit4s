package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.ActivityMsg
import fit4s.profile.types.DateTime
import fit4s.profile.types

import java.time.Duration

final case class Activity(
    timestamp: DateTime,
    totalTime: Duration,
    numSessions: Int,
    activityType: Option[types.Activity]
)

object Activity {

  def from(activityMsg: DataMessage): Either[String, Activity] =
    if (!activityMsg.isMessage(ActivityMsg))
      Left(s"Not an activity message: $activityMsg")
    else
      for {
        time <- activityMsg.getRequiredField(ActivityMsg.timestamp)
        total <- activityMsg.getRequiredField(ActivityMsg.totalTimerTime)
        totalDuration <- total.duration.toRight(s"Invalid total duration: $total")
        num <- activityMsg.getRequiredField(ActivityMsg.numSessions)
        at <- activityMsg.getField(ActivityMsg.`type`)
      } yield Activity(time.value, totalDuration, num.value.rawValue.toInt, at.map(_.value))
}
