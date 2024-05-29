package fit4s.tcx

import java.time.Duration
import java.time.Instant

import scala.collection.immutable.Seq
import scala.xml.Node

import fit4s.data.*
import fit4s.profile.types.Sport

object TcxReader:
  def activities(n: Node): Seq[TcxActivity] =
    (n \ "Activities" \ "Activity").map(activity)

  def activity(n: Node): TcxActivity =
    TcxActivity(
      id = Instant.parse((n \ "Id").text.trim),
      sport = sport(n \@ "Sport"),
      laps = (n \ "Lap").map(lap),
      creator = (n \ "Creator").headOption.map(creator)
    )

  def lap(n: Node) = TcxLap(
    startTime = Instant.parse(n \@ "StartTime"),
    totalTime = Duration.ofMillis(((n \ "TotalTimeSeconds").text.toDouble * 1000).toLong),
    distance = Distance.meter((n \ "DistanceMeters").text.toDouble),
    maximumSpeed =
      (n \ "MaximumSpeed").headOption.map(e => Speed.meterPerSecond(e.text.toDouble)),
    calories = Calories.kcal((n \ "Calories").text.toInt),
    averageHeartRate =
      (n \ "AverageHeartRateBpm").headOption.map(_.text.trim.toInt).map(HeartRate.bpm),
    maximumHeartRate =
      (n \ "MaximumHeartRateBpm").headOption.map(_.text.trim.toInt).map(HeartRate.bpm),
    cadence = (n \ "Cadence").headOption.map(_.text.trim.toInt).map(Cadence.rpm),
    track = (n \ "Track" \ "Trackpoint").map(trackpoint)
  )

  def creator(n: Node) = TcxCreator(
    name = (n \ "Name").text.trim,
    unitId = (n \ "UnitId").text.trim.toLong,
    productId = (n \ "ProductID").text.trim.toInt
  )

  private def trackpoint(n: Node): TcxTrackpoint = TcxTrackpoint(
    time = Instant.parse((n \ "Time").head.text.trim),
    position = (n \ "Position").headOption.map(position),
    altitude =
      (n \ "AltitudeMeters").headOption.map(n => Distance.meter(n.text.trim.toDouble)),
    distance =
      (n \ "DistanceMeters").headOption.map(n => Distance.meter(n.text.trim.toDouble)),
    heartRate =
      (n \ "HeartRateBpm").headOption.map(n => HeartRate.bpm(n.text.trim.toInt)),
    cadence = (n \ "Cadence").headOption.map(n => Cadence.rpm(n.text.trim.toInt))
  )

  private def position(n: Node): Position =
    Position.degree(
      lat = (n \ "LatitudeDegrees").head.text.trim.toDouble,
      lng = (n \ "LongitudeDegrees").head.text.trim.toDouble
    )

  private def sport(s: String): Sport =
    Sport.all
      .find(e => e.typeName.equalsIgnoreCase(s))
      .orElse(Option.when("biking".equalsIgnoreCase(s))(Sport.Cycling))
      .getOrElse(Sport.Generic)
