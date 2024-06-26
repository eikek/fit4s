package fit4s.data

import java.time.Duration

import scala.math.Ordering.Implicits.*

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.SessionMsg
import fit4s.profile.types.*

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

  def from(sessionMsg: DataMessage): Either[String, FitActivitySession] =
    if (!sessionMsg.isMessage(SessionMsg)) Left(s"Not a session message: $sessionMsg")
    else
      for {
        sport <- sessionMsg.getField(SessionMsg.sport)
        subSport <- sessionMsg.getField(SessionMsg.subSport)
        start <- sessionMsg.getRequiredField(SessionMsg.startTime)
        end <- sessionMsg.getRequiredField(SessionMsg.timestamp)
        movingTime1 <- sessionMsg.getField(SessionMsg.totalMovingTime)
        movingTime2 <- sessionMsg.getField(SessionMsg.totalTimerTime)
        movingTime = movingTime1
          .flatMap(_.duration)
          .orElse(movingTime2.flatMap(_.duration))
        elapsedTime <- sessionMsg.getField(SessionMsg.totalElapsedTime)
        kcal <- sessionMsg.getField(SessionMsg.totalCalories)
        distance <- sessionMsg.getField(SessionMsg.totalDistance)
        maxSpeed <- sessionMsg.getField(SessionMsg.maxSpeed)
        avgSpeed <- sessionMsg.getField(SessionMsg.avgSpeed)
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
        start.value,
        end.value,
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
