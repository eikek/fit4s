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
    minTemp: Option[Temperature],
    maxTemp: Option[Temperature],
    avgTemp: Option[Temperature],
    maxSpeed: Speed,
    avgSpeed: Option[Speed],
    minHr: Option[HeartRate],
    maxHr: Option[HeartRate],
    avgHr: Option[HeartRate],
    maxPower: Option[Power],
    avgPower: Option[Power]
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
      minTemp = select(minTemp, other.minTemp, Ordering[Temperature].min[Temperature]),
      maxTemp = select(maxTemp, other.maxTemp, Ordering[Temperature].max[Temperature]),
      avgTemp,
      maxSpeed = Ordering[Speed].max(maxSpeed, other.maxSpeed),
      avgSpeed,
      minHr = select(minHr, other.minHr, Ordering[HeartRate].min[HeartRate]),
      maxHr = select(maxHr, other.maxHr, Ordering[HeartRate].max[HeartRate]),
      avgHr,
      maxPower = select(maxPower, other.maxPower, Ordering[Power].max[Power]),
      avgPower
    )

  private def select[A](
      a: Option[A],
      b: Option[A],
      winner: (A, A) => A
  ): Option[A] =
    (a, b) match {
      case (Some(x), Some(y)) => Some(winner(x, y))
      case (Some(_), None)    => a
      case (None, Some(_))    => b
      case (None, None)       => None
    }

  override def toString: String =
    s"ActivitySummary(id=$id, sport=$sport, subSport=$subSport, start=$startTime, moving=$movingTime, " +
      s"elapsed=$elapsedTime, calories=$calories, distance=$distance, minTemp=$minTemp, maxTemp=$maxTemp, " +
      s"avgTemp=$avgTemp, maxSpeed=$maxSpeed, avgSpeed=${avgSpeed.getOrElse("n.a.")}, " +
      s"minHr=$minHr, maxHr=$maxHr, avgHr=${avgHr.getOrElse("n.a")}, maxPower=$maxPower, avgPower=${avgPower
          .getOrElse("n.a")})"
}

object ActivitySummary {

  def from(id: FileId, sessionMsg: DataMessage): Either[String, ActivitySummary] =
    for {
      sport <- sessionMsg.getField(SessionMsg.sport)
      subSport <- sessionMsg.getField(SessionMsg.subSport)
      start <- sessionMsg.getField(SessionMsg.startTime)
      movingTime <- sessionMsg.getField(SessionMsg.totalMovingTime)
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
    } yield ActivitySummary(
      id,
      sport.map(_.value).getOrElse(Sport.Generic),
      subSport.map(_.value).getOrElse(SubSport.Generic),
      start.map(_.value.asInstant).getOrElse(Instant.MIN),
      movingTime.flatMap(_.duration).getOrElse(Duration.ZERO),
      elapsedTime.flatMap(_.duration).getOrElse(Duration.ZERO),
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
      avgPower.flatMap(_.power)
    )

  def from(fit: FitFile): Either[String, ActivitySummary] = for {
    fileId <- fit.findFirstData(MesgNum.FileId).flatMap(FileId.from)
    activities <- fit.findData(MesgNum.Session).mapEither(from(fileId, _))
    result <-
      if (activities.isEmpty) Left(s"No session message found")
      else Right(activities.reduce(_ combine _))
    // estimate averages...
    avgSpeed = makeAvg(activities.flatMap(_.avgSpeed.map(_.meterPerSecond)))
      .map(Speed.meterPerSecond)
    avgTemp = makeAvg(activities.flatMap(_.avgTemp.map(_.celcius)))
      .map(Temperature.celcius)
    avgHr = makeAvg(activities.flatMap(_.avgHr.map(_.bpm.toDouble)))
      .map(_.toInt)
      .map(HeartRate.bpm)
    avgPower = makeAvg(activities.flatMap(_.avgPower.map(_.watts)))
      .map(_.toInt)
      .map(Power.watts)
  } yield result.copy(
    avgSpeed = avgSpeed,
    avgTemp = avgTemp,
    avgHr = avgHr,
    avgPower = avgPower
  )

  private def makeAvg(values: Vector[Double]) =
    Option.when(values.nonEmpty) {
      val sum = values.sum
      sum / values.size
    }
}