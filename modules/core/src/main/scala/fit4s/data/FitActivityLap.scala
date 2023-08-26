package fit4s.data

import java.time.Duration

import scala.math.Ordering.Implicits._

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.LapMsg
import fit4s.profile.types._

final case class FitActivityLap(
    sport: Sport,
    subSport: SubSport,
    trigger: Option[LapTrigger],
    startTime: DateTime,
    endTime: DateTime,
    startPosition: Option[Position],
    endPosition: Option[Position],
    movingTime: Option[Duration],
    elapsedTime: Option[Duration],
    calories: Calories,
    distance: Distance,
    minTemp: Option[Temperature],
    maxTemp: Option[Temperature],
    avgTemp: Option[Temperature],
    maxSpeed: Option[Speed],
    avgSpeed: Option[Speed],
    minHr: Option[HeartRate],
    maxHr: Option[HeartRate],
    avgHr: Option[HeartRate],
    maxPower: Option[Power],
    avgPower: Option[Power],
    normPower: Option[Power],
    maxCadence: Option[Cadence],
    avgCadence: Option[Cadence],
    totalAscend: Option[Distance],
    totalDescend: Option[Distance],
    numPoolLength: Option[Int],
    swimStroke: Option[SwimStroke],
    avgStrokeDistance: Option[Distance],
    strokeCount: Option[Int],
    avgGrade: Option[Percent]
) {

  def contains(dt: DateTime): Boolean =
    startTime <= dt && dt <= endTime
}

object FitActivityLap {

  def from(lapMsg: DataMessage): Either[String, FitActivityLap] =
    if (!lapMsg.isMessage(LapMsg)) Left(s"Not a session message: $lapMsg")
    else
      for {
        sport <- lapMsg.getField(LapMsg.sport)
        subSport <- lapMsg.getField(LapMsg.subSport)
        trigger <- lapMsg.getField(LapMsg.lapTrigger)
        start <- lapMsg.getRequiredField(LapMsg.startTime)
        end <- lapMsg.getRequiredField(LapMsg.timestamp)
        movingTime1 <- lapMsg.getField(LapMsg.totalMovingTime)
        movingTime2 <- lapMsg.getField(LapMsg.totalTimerTime)
        movingTime = movingTime1
          .flatMap(_.duration)
          .orElse(movingTime2.flatMap(_.duration))
        elapsedTime <- lapMsg.getField(LapMsg.totalElapsedTime)
        kcal <- lapMsg.getField(LapMsg.totalCalories)
        distance <- lapMsg.getField(LapMsg.totalDistance)
        maxSpeed <- lapMsg.getField(LapMsg.maxSpeed)
        avgSpeed <- lapMsg.getField(LapMsg.avgSpeed)
        maxHr <- lapMsg.getField(LapMsg.maxHeartRate)
        minHr <- lapMsg.getField(LapMsg.minHeartRate)
        avgHr <- lapMsg.getField(LapMsg.avgHeartRate)
        minTemp <- lapMsg.getField(LapMsg.minTemperature)
        maxTemp <- lapMsg.getField(LapMsg.maxTemperature)
        avgTemp <- lapMsg.getField(LapMsg.avgTemperature)
        maxPower <- lapMsg.getField(LapMsg.maxPower)
        avgPower <- lapMsg.getField(LapMsg.avgPower)
        normPower <- lapMsg.getField(LapMsg.normalizedPower)
        maxCad <- lapMsg.getField(LapMsg.maxCadence)
        avgCad <- lapMsg.getField(LapMsg.avgCadence)
        asc <- lapMsg.getField(LapMsg.totalAscent)
        desc <- lapMsg.getField(LapMsg.totalDescent)
        startPosLat <- lapMsg.getField(LapMsg.startPositionLat)
        startPosLng <- lapMsg.getField(LapMsg.startPositionLong)
        endPosLat <- lapMsg.getField(LapMsg.endPositionLat)
        endPosLng <- lapMsg.getField(LapMsg.endPositionLong)
        poolLenNum <- lapMsg.getField(LapMsg.numLengths)
        swimStroke <- lapMsg.getField(LapMsg.swimStroke)
        avgStrokeDst <- lapMsg.getField(LapMsg.avgStrokeDistance)
        strokeCnt <- lapMsg.getField(LapMsg.strokeCount)
        avgGrade <- lapMsg.getField(LapMsg.avgGrade)
      } yield FitActivityLap(
        sport.map(_.value).getOrElse(Sport.Generic),
        subSport.map(_.value).getOrElse(SubSport.Generic),
        trigger.map(_.value),
        start.value,
        end.value,
        Position.optional(
          startPosLat.flatMap(_.semicircle),
          startPosLng.flatMap(_.semicircle)
        ),
        Position.optional(
          endPosLat.flatMap(_.semicircle),
          endPosLng.flatMap(_.semicircle)
        ),
        movingTime,
        elapsedTime.flatMap(_.duration),
        kcal.flatMap(_.calories).getOrElse(Calories.zero),
        distance.flatMap(_.distance).getOrElse(Distance.zero),
        minTemp.flatMap(_.temperature),
        maxTemp.flatMap(_.temperature),
        avgTemp.flatMap(_.temperature),
        maxSpeed.flatMap(_.speed),
        avgSpeed.flatMap(_.speed),
        minHr.flatMap(_.heartrate),
        maxHr.flatMap(_.heartrate),
        avgHr.flatMap(_.heartrate),
        maxPower.flatMap(_.power),
        avgPower.flatMap(_.power),
        normPower.flatMap(_.power),
        maxCad.flatMap(_.cadence),
        avgCad.flatMap(_.cadence),
        asc.flatMap(_.distance),
        desc.flatMap(_.distance),
        poolLenNum.flatMap(_.asLong).map(_.toInt),
        swimStroke.map(_.value),
        avgStrokeDst.flatMap(_.distance),
        strokeCnt.flatMap(_.counts),
        avgGrade.flatMap(_.percent)
      )
}
