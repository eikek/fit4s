package fit4s.tcx

import java.time.Duration
import java.time.Instant

import scala.collection.immutable.Seq

import fit4s.data.*

final case class TcxLap(
    startTime: Instant,
    totalTime: Duration,
    distance: Distance,
    maximumSpeed: Option[Speed],
    calories: Calories,
    averageHeartRate: Option[HeartRate],
    maximumHeartRate: Option[HeartRate],
    cadence: Option[Cadence],
    track: Seq[TcxTrackpoint]
):

  val endTime = startTime.plus(totalTime)

  val startPosition = track.headOption.flatMap(_.position)
  val endPosition = track.lastOption.flatMap(_.position)

  val (totalAscend, totalDescend) =
    val alts = track.flatMap(_.altitude).map(_.meter)
    if (alts.isEmpty) (Distance.zero, Distance.zero)
    else
      val (a, d) = alts.zip(alts.tail).foldLeft((0d, 0d)) { case ((up, down), (a, b)) =>
        val diff = b - a
        if (diff > 0) (up + diff, down)
        else (up, down + diff.abs)
      }
      (Distance.meter(a), Distance.meter(d))

  val avgSpeed: Option[Speed] =
    Option.when(totalTime.toSeconds > 0)(
      Speed.meterPerSecond(distance.meter / totalTime.toSeconds())
    )

  val avgHr: Option[HeartRate] =
    val e = track.flatMap(_.heartRate)
    Option.when(e.nonEmpty)(HeartRate.bpm(e.map(_.bpm).sum / e.size))
