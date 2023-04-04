package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.ActivityMsg
import fit4s.profile.types.{DateTime, LocalDateTime}
import fit4s.profile.types

import java.time.Duration

final case class Activity(
    timestamp: DateTime,
    localTimestamp: Option[LocalDateTime],
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
        localTs <- activityMsg.getField(ActivityMsg.localTimestamp)
        total <- activityMsg.getField(ActivityMsg.totalTimerTime)
        totalDuration <- total
          .map(_.duration.toRight(s"Invalid total duration: $total"))
          .getOrElse(Right(Duration.ZERO))
        num <- activityMsg.getField(ActivityMsg.numSessions)
        at <- activityMsg.getField(ActivityMsg.`type`)
      } yield Activity(
        time.value,
        localTs.map(_.value),
        totalDuration,
        num.map(_.value.rawValue.toInt).getOrElse(1),
        at.map(_.value)
      )
}