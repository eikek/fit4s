package fit4s.data

import fit4s.FitFile
import fit4s.profile.messages.SessionMsg
import fit4s.profile.types.{MesgNum, Sport}
import fit4s.util._

import java.time.{Duration, Instant}

final case class ActivitySummary(
    device: DeviceProduct,
    sport: Sport,
    startTime: Instant,
    movingTime: Duration,
    elapsedTime: Duration,
    distance: Distance,
    minTemp: Temperature,
    maxTemp: Temperature,
    avgTemp: Option[Temperature],
    maxSpeed: Speed,
    avgSpeed: Option[Speed],
    minHr: HeartRate,
    maxHr: HeartRate,
    avgHr: Option[HeartRate]
) {

  def combine(other: ActivitySummary): ActivitySummary =
    ActivitySummary(
      device = device,
      sport =
        if (sport == Sport.Generic) other.sport
        else sport,
      startTime = if (startTime.isBefore(other.startTime)) startTime else other.startTime,
      movingTime = movingTime.plus(other.movingTime),
      elapsedTime = elapsedTime.plus(other.elapsedTime),
      distance = distance + other.distance,
      minTemp = Ordering[Temperature].min(minTemp, other.minTemp),
      maxTemp = Ordering[Temperature].max(maxTemp, other.maxTemp),
      avgTemp = (avgTemp, other.avgTemp) match {
        case (Some(a), Some(b)) => Some((a + b) / 2)
        case (Some(a), _)       => Some(a)
        case (_, Some(b))       => Some(b)
        case (_, _)             => None
      },
      maxSpeed = Ordering[Speed].max(maxSpeed, other.maxSpeed),
      avgSpeed,
      minHr = Ordering[HeartRate].min(minHr, other.minHr),
      maxHr = Ordering[HeartRate].max(maxHr, other.maxHr),
      avgHr
    )

  override def toString =
    s"ActivitySummary(device=$device, sport=$sport, start=$startTime, moving=$movingTime, " +
      s"elapsed=$elapsedTime, distance=$distance, minTemp=$minTemp, maxTemp=$maxTemp, " +
      s"avgTemp=$avgTemp, maxSpeed=$maxSpeed, avgSpeed=${avgSpeed.getOrElse("n.a.")}, " +
      s"minHr=$minHr, maxHr=$maxHr, avgHr=${avgHr.getOrElse("n.a")})"
}

object ActivitySummary {

  def from(fit: FitFile): Either[String, ActivitySummary] = {
    val device = fit.findFirstData(MesgNum.FileId).flatMap(DeviceProduct.from)
    fit
      .findData(MesgNum.Session)
      .mapEither(sm =>
        for {
          sport <- sm.findField(SessionMsg.sport)
          start <- sm.findField(SessionMsg.startTime)
          movingTime <- sm.findField(SessionMsg.totalMovingTime)
          elapsedTime <- sm.findField(SessionMsg.totalElapsedTime)
          distance <- sm.findField(SessionMsg.totalDistance)
          maxSpeed <- sm.findField(SessionMsg.maxSpeed)
          avgSpeed <- sm.findField(SessionMsg.avgSpeed)
          maxHr <- sm.findField(SessionMsg.maxHeartRate)
          minHr <- sm.findField(SessionMsg.minHeartRate)
          avgHr <- sm.findField(SessionMsg.avgHeartRate)
          minTemp <- sm.findField(SessionMsg.minTemperature)
          maxTemp <- sm.findField(SessionMsg.maxTemperature)
          avgTemp <- sm.findField(SessionMsg.avgTemperature)
        } yield ActivitySummary(
          DeviceProduct.Unknown,
          sport.map(_.value).getOrElse(Sport.Generic),
          start.map(_.value.asInstant).getOrElse(Instant.MIN),
          movingTime.flatMap(_.duration).getOrElse(Duration.ZERO),
          elapsedTime.flatMap(_.duration).getOrElse(Duration.ZERO),
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
      )
      .flatMap(a =>
        if (a.isEmpty) Left(s"No session mesg found") else Right(a.reduce(_ combine _))
      )
      .flatMap(summary => device.map(dev => summary.copy(device = dev)))
  }
}
