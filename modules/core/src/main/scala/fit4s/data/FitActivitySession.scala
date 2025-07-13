package fit4s.data

import java.time.Duration

import scala.math.Ordering.Implicits.*

import fit4s.FitMessage.DataMessage
import fit4s.profile.FieldValue
import fit4s.profile.messages.SessionMsg
import fit4s.profile.types.*
import fit4s.profile.types.BaseTypedValue.LongBaseValue

final case class FitActivitySession(
    sport: Sport,
    subSport: SubSport,
    startTime: DateTime,
    endTime: DateTime,
    movingTime: Option[Duration],
    elapsedTime: Option[Duration],
    calories: Calories,
    distance: Distance,
    minTemp: Option[Temperature],
    maxTemp: Option[Temperature],
    avgTemp: Option[Temperature],
    maxSpeed: Speed,
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
    startPosition: Option[Position],
    trainingStressScore: Option[TrainingStressScore],
    numPoolLength: Option[Int],
    intensityFactor: Option[IntensityFactor],
    swimStroke: Option[SwimStroke],
    avgStrokeDistance: Option[Distance],
    avgStrokeCount: Option[StrokesPerLap],
    poolLength: Option[Distance],
    avgGrade: Option[Percent]
):

  def containsTime(dt: DateTime): Boolean =
    startTime <= dt && dt <= endTime

object FitActivitySession:

  private def figureEndTime(
      startTime: FieldValue[DateTime],
      endTime: FieldValue[DateTime],
      elapsed: Option[FieldValue[LongBaseValue]]
  ): FieldValue[DateTime] = elapsed.flatMap(_.duration) match
    case None    => endTime
    case Some(_) if endTime.value > startTime.value => endTime
    case Some(v) =>
      val started = startTime.value.asInstant
      val span = java.time.Duration.between(DateTime.offset, started).plus(v)
      val secs = span.toSeconds()
      endTime.copy(value = DateTime(secs))

  def from(sessionMsg: DataMessage): Either[String, FitActivitySession] =
    if (!sessionMsg.isMessage(SessionMsg)) Left(s"Not a session message: $sessionMsg")
    else
      for {
        sport <- sessionMsg.getField(SessionMsg.sport)
        subSport <- sessionMsg.getField(SessionMsg.subSport)
        startTime <- sessionMsg.getRequiredField(SessionMsg.startTime)
        endTime1 <- sessionMsg.getRequiredField(SessionMsg.timestamp)
        movingTime1 <- sessionMsg.getField(SessionMsg.totalMovingTime)
        movingTime2 <- sessionMsg.getField(SessionMsg.totalTimerTime)
        movingTime = movingTime1
          .flatMap(_.duration)
          .orElse(movingTime2.flatMap(_.duration))
        elapsedTime <- sessionMsg.getField(SessionMsg.totalElapsedTime)
        endTime = figureEndTime(startTime, endTime1, elapsedTime)
        kcal <- sessionMsg.getField(SessionMsg.totalCalories)
        distance <- sessionMsg.getField(SessionMsg.totalDistance)
        maxSpeed1 <- sessionMsg.getField(SessionMsg.enhancedMaxSpeed)
        maxSpeed2 <- sessionMsg.getField(SessionMsg.maxSpeed)
        maxSpeed = maxSpeed1.orElse(maxSpeed2)
        avgSpeed1 <- sessionMsg.getField(SessionMsg.enhancedAvgSpeed)
        avgSpeed2 <- sessionMsg.getField(SessionMsg.avgSpeed)
        avgSpeed = avgSpeed1.orElse(avgSpeed2)
        maxHr <- sessionMsg.getField(SessionMsg.maxHeartRate)
        minHr <- sessionMsg.getField(SessionMsg.minHeartRate)
        avgHr <- sessionMsg.getField(SessionMsg.avgHeartRate)
        minTemp <- sessionMsg.getField(SessionMsg.minTemperature)
        maxTemp <- sessionMsg.getField(SessionMsg.maxTemperature)
        avgTemp <- sessionMsg.getField(SessionMsg.avgTemperature)
        maxPower <- sessionMsg.getField(SessionMsg.maxPower)
        avgPower <- sessionMsg.getField(SessionMsg.avgPower)
        normPower <- sessionMsg.getField(SessionMsg.normalizedPower)
        maxCad <- sessionMsg.getField(SessionMsg.maxCadence)
        avgCad <- sessionMsg.getField(SessionMsg.avgCadence)
        asc <- sessionMsg.getField(SessionMsg.totalAscent)
        desc <- sessionMsg.getField(SessionMsg.totalDescent)
        startPosLat <- sessionMsg.getField(SessionMsg.startPositionLat)
        startPosLng <- sessionMsg.getField(SessionMsg.startPositionLong)
        tss <- sessionMsg.getField(SessionMsg.trainingStressScore)
        poolLenNum <- sessionMsg.getField(SessionMsg.numLengths)
        iff <- sessionMsg.getField(SessionMsg.intensityFactor)
        swimStroke <- sessionMsg.getField(SessionMsg.swimStroke)
        avgStrokeDst <- sessionMsg.getField(SessionMsg.avgStrokeDistance)
        avgStrokeCnt <- sessionMsg.getField(SessionMsg.avgStrokeCount)
        poolLength <- sessionMsg.getField(SessionMsg.poolLength)
        avgGrade <- sessionMsg.getField(SessionMsg.avgGrade)
      } yield FitActivitySession(
        sport.map(_.value).getOrElse(Sport.Generic),
        subSport.map(_.value).getOrElse(SubSport.Generic),
        startTime.value,
        endTime.value,
        movingTime,
        elapsedTime.flatMap(_.duration),
        kcal.flatMap(_.calories).getOrElse(Calories.zero),
        distance.flatMap(_.distance).getOrElse(Distance.zero),
        minTemp.flatMap(_.temperature),
        maxTemp.flatMap(_.temperature),
        avgTemp.flatMap(_.temperature),
        maxSpeed.flatMap(_.speed).getOrElse(Speed.zero),
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
        Position.optional(
          startPosLat.flatMap(_.semicircle),
          startPosLng.flatMap(_.semicircle)
        ),
        tss.flatMap(_.trainingStressScore),
        poolLenNum.flatMap(_.asLong).map(_.toInt),
        iff.flatMap(_.intensityFactor),
        swimStroke.map(_.value),
        avgStrokeDst.flatMap(_.distance),
        avgStrokeCnt.flatMap(_.strokesPerLap),
        poolLength.flatMap(_.distance),
        avgGrade.flatMap(_.percent)
      )
