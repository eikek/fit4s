package fit4s.data

import fit4s.FitFile
import fit4s.profile.messages.{EventMsg, FileIdMsg, RecordMsg, SessionMsg, SportMsg}
import fit4s.profile.types.{Event, EventType, MesgNum, Sport}
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

  private case class RunningTotal(
      device: DeviceProduct = DeviceProduct.Unknown,
      sport: Sport = Sport.Generic,
      startTime: Instant = Instant.MIN,
      endTime: Instant = Instant.MIN,
      distanceMeter: Distance = Distance.zero,
      minTemp: Temperature = Temperature.maxValue,
      maxTemp: Temperature = Temperature.minValue,
      sumTemp: Temperature = Temperature.zero,
      countTemp: Int = 0,
      maxSpeed: Speed = Speed.minValue,
      sumSpeed: Speed = Speed.zero,
      countSpeed: Int = 0,
      minHr: HeartRate = HeartRate.maxValue,
      maxHr: HeartRate = HeartRate.minValue,
      sumHr: HeartRate = HeartRate.zero,
      countHr: Int = 0
  ) {
    def setHr(hr: HeartRate): RunningTotal =
      copy(
        minHr = Ordering[HeartRate].min(minHr, hr),
        maxHr = Ordering[HeartRate].max(maxHr, hr),
        sumHr = sumHr + hr,
        countHr = countHr + 1
      )

    def setSpeed(spd: Speed): RunningTotal =
      copy(
        maxSpeed = Ordering[Speed].max(maxSpeed, spd),
        sumSpeed = sumSpeed + spd,
        countSpeed = countSpeed + 1
      )

    def setTemp(temp: Temperature): RunningTotal =
      copy(
        minTemp = Ordering[Temperature].min(minTemp, temp),
        maxTemp = Ordering[Temperature].max(maxTemp, temp),
        countTemp = countTemp + 1
      )

    def setDistance(d: Distance): RunningTotal = copy(distanceMeter = d)

    def toSummary: Either[String, ActivitySummary] = {
      val empty = RunningTotal()
      if (startTime == empty.startTime) Left("No startTime found.")
      else if (endTime == empty.endTime) Left("No endTime found.")
      else if (distanceMeter == empty.distanceMeter) Left("No distance found")
      else
        Right(
          ActivitySummary(
            device,
            sport,
            startTime,
            Duration.ZERO,
            Duration.ZERO,
            distanceMeter,
            minTemp,
            maxTemp,
            Option.when(countTemp > 0)(sumTemp / countTemp),
            maxSpeed,
            Option.when(countSpeed > 0)(sumSpeed / countSpeed),
            minHr,
            maxHr,
            Option.when(countHr > 0)(sumHr / countHr)
          )
        )
    }

  }

  def from2(fit: FitFile): Either[String, ActivitySummary] = {
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
      .map(_.reduce(_ combine _))
      .flatMap(summary => device.map(dev => summary.copy(device = dev)))
  }

  def from(fit: FitFile): Either[String, ActivitySummary] =
    fit.dataRecords
      .foldLeft(RunningTotal().asRight[String]) { (totalsEither, dm) =>
        for {
          totals <- totalsEither
          next <- dm.definition.profileMsg match {
            case Some(FileIdMsg) =>
              DeviceProduct.from(dm).map(d => totals.copy(device = d))

            case Some(SportMsg) =>
              dm.findField(SportMsg.sport)
                .map(_.map(s => totals.copy(sport = s.value)).getOrElse(totals))

            case Some(EventMsg) if dm.isEvent(Event.Timer, EventType.Start) =>
              dm.findField(EventMsg.timestamp)
                .map(
                  _.map(dt => totals.copy(startTime = dt.value.asInstant))
                    .getOrElse(totals)
                )

            case Some(EventMsg) if dm.isEvent(Event.Timer, EventType.StopAll) =>
              dm.findField(EventMsg.timestamp)
                .map(
                  _.map(dt => totals.copy(endTime = dt.value.asInstant))
                    .getOrElse(totals)
                )

            case Some(RecordMsg) =>
              for {
                dist <- dm.findField(RecordMsg.distance)
                spd <- dm.findField(RecordMsg.speed)
                temp <- dm.findField(RecordMsg.temperature)
                hr <- dm.findField(RecordMsg.heartRate)
                updates = List[RunningTotal => Option[RunningTotal]](
                  t => dist.flatMap(_.distance).map(t.setDistance),
                  t => spd.flatMap(_.speed).map(t.setSpeed),
                  t => temp.flatMap(_.temperature).map(t.setTemp),
                  t => hr.flatMap(_.heartrate).map(t.setHr)
                )
              } yield updates.foldLeft(totals)((r, el) => el(r).getOrElse(r))

            case _ =>
              Right(totals)
          }
        } yield next
      }
      .flatMap(_.toSummary)
}
