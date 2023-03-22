package fit4s.data

import fit4s.FitFile
import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.SessionMsg
import fit4s.profile.types.{MesgNum, Sport, SubSport}
import fit4s.util._

import java.time.{Duration, Instant}

final case class ActivitySummary(
    id: FileId,
    sport: Sport,
    subSport: SubSport,
    startTime: Instant,
    movingTime: Duration,
    elapsedTime: Duration,
    calories: Calories,
    distance: Distance,
    minTemp: Temperature, // TODO make optional!
    maxTemp: Temperature,
    avgTemp: Option[Temperature],
    maxSpeed: Speed,
    avgSpeed: Option[Speed],
    minHr: HeartRate,
    maxHr: HeartRate,
    avgHr: Option[HeartRate]
) {

  private def combine(other: ActivitySummary): ActivitySummary =
    ActivitySummary(
      id = id,
      sport =
        if (sport == Sport.Generic) other.sport
        else sport,
      subSport =
        if (subSport == SubSport.Generic) other.subSport
        else
          subSport,
      startTime = if (startTime.isBefore(other.startTime)) startTime else other.startTime,
      movingTime = movingTime.plus(other.movingTime),
      elapsedTime = elapsedTime.plus(other.elapsedTime),
      calories = calories + other.calories,
      distance = distance + other.distance,
      minTemp = Ordering[Temperature].min(minTemp, other.minTemp),
      maxTemp = Ordering[Temperature].max(maxTemp, other.maxTemp),
      avgTemp,
      maxSpeed = Ordering[Speed].max(maxSpeed, other.maxSpeed),
      avgSpeed,
      minHr = Ordering[HeartRate].min(minHr, other.minHr),
      maxHr = Ordering[HeartRate].max(maxHr, other.maxHr),
      avgHr
    )

  override def toString =
    s"ActivitySummary(id=$id, sport=$sport, subSport=$subSport, start=$startTime, moving=$movingTime, " +
      s"elapsed=$elapsedTime, calories=$calories, distance=$distance, minTemp=$minTemp, maxTemp=$maxTemp, " +
      s"avgTemp=$avgTemp, maxSpeed=$maxSpeed, avgSpeed=${avgSpeed.getOrElse("n.a.")}, " +
      s"minHr=$minHr, maxHr=$maxHr, avgHr=${avgHr.getOrElse("n.a")})"
}

object ActivitySummary {

  def from(id: FileId, sessionMsg: DataMessage): Either[String, ActivitySummary] =
    for {
      sport <- sessionMsg.findField(SessionMsg.sport)
      subSport <- sessionMsg.findField(SessionMsg.subSport)
      start <- sessionMsg.findField(SessionMsg.startTime)
      movingTime <- sessionMsg.findField(SessionMsg.totalMovingTime)
      elapsedTime <- sessionMsg.findField(SessionMsg.totalElapsedTime)
      kcal <- sessionMsg.findField(SessionMsg.totalCalories)
      distance <- sessionMsg.findField(SessionMsg.totalDistance)
      maxSpeed <- sessionMsg.findField(SessionMsg.maxSpeed)
      avgSpeed <- sessionMsg.findField(SessionMsg.avgSpeed)
      maxHr <- sessionMsg.findField(SessionMsg.maxHeartRate)
      minHr <- sessionMsg.findField(SessionMsg.minHeartRate)
      avgHr <- sessionMsg.findField(SessionMsg.avgHeartRate)
      minTemp <- sessionMsg.findField(SessionMsg.minTemperature)
      maxTemp <- sessionMsg.findField(SessionMsg.maxTemperature)
      avgTemp <- sessionMsg.findField(SessionMsg.avgTemperature)
    } yield ActivitySummary(
      id,
      sport.map(_.value).getOrElse(Sport.Generic),
      subSport.map(_.value).getOrElse(SubSport.Generic),
      start.map(_.value.asInstant).getOrElse(Instant.MIN),
      movingTime.flatMap(_.duration).getOrElse(Duration.ZERO),
      elapsedTime.flatMap(_.duration).getOrElse(Duration.ZERO),
      kcal.flatMap(_.calories).getOrElse(Calories.zero),
      distance.flatMap(_.distance).getOrElse(Distance.zero),
      minTemp.flatMap(_.temperature).getOrElse(Temperature.minValue),
      maxTemp.flatMap(_.temperature).getOrElse(Temperature.maxValue),
      Some(avgTemp.flatMap(_.temperature).getOrElse(Temperature.zero)),
      maxSpeed.flatMap(_.speed).getOrElse(Speed.zero),
      Some(avgSpeed.flatMap(_.speed).getOrElse(Speed.zero)),
      minHr.flatMap(_.heartrate).getOrElse(HeartRate.zero),
      maxHr.flatMap(_.heartrate).getOrElse(HeartRate.zero),
      Some(avgHr.flatMap(_.heartrate).getOrElse(HeartRate.zero))
    )

  def from(fit: FitFile): Either[String, ActivitySummary] = for {
    fileId <- fit.findFirstData(MesgNum.FileId).flatMap(FileId.from)
    activities <- fit.findData(MesgNum.Session).mapEither(from(fileId, _))
    result <-
      if (activities.isEmpty) Left(s"No session message found")
      else Right(activities.reduce(_ combine _))
    // estimate averages...
    // TODO no average when Option.empty!
    avgSpeed = Option
      .when(activities.nonEmpty)(
        activities.flatMap(_.avgSpeed.map(_.meterPerSecond)).sum / activities.size
      )
      .map(Speed.meterPerSecond)
    avgTemp = Option
      .when(activities.nonEmpty)(
        activities.flatMap(_.avgTemp.map(_.celcius)).sum / activities.size
      )
      .map(Temperature.celcius)
    avgHr = Option
      .when(activities.nonEmpty)(
        activities.flatMap(_.avgHr.map(_.bpm)).sum / activities.size
      )
      .map(HeartRate.bpm)
  } yield result.copy(avgSpeed = avgSpeed, avgTemp = avgTemp, avgHr = avgHr)
}
