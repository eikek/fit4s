package fit4s.activities.data

import java.time.{Duration, Instant}

import cats.Semigroup
import cats.data.NonEmptyList
import cats.syntax.all.*

import fit4s.data.*
import fit4s.profile.types.*

final case class ActivitySessionSummary(
    sport: Sport,
    startTime: Instant,
    endTime: Instant,
    movingTime: Duration,
    elapsedTime: Duration,
    distance: Distance,
    calories: Calories,
    totalAscend: Option[Distance],
    totalDescend: Option[Distance],
    minTemp: Option[Temperature],
    maxTemp: Option[Temperature],
    avgTemp: Option[Temperature],
    minHr: Option[HeartRate],
    maxHr: Option[HeartRate],
    avgHr: Option[HeartRate],
    maxSpeed: Option[Speed],
    avgSpeed: Option[Speed],
    maxPower: Option[Power],
    avgPower: Option[Power],
    avgNormPower: Option[Power],
    avgCadence: Option[Cadence],
    avgGrade: Option[Percent],
    avgIntensity: Option[IntensityFactor],
    avgTss: Option[TrainingStressScore],
    count: Int
)

object ActivitySessionSummary {

  def combine(ass: NonEmptyList[ActivitySessionSummary]): ActivitySessionSummary =
    if (ass.tail.isEmpty) ass.head
    else {
      val len = ass.length.toDouble
      ActivitySessionSummary(
        ass.head.sport,
        ass.map(_.startTime).toList.min,
        ass.map(_.endTime).toList.max,
        ass.map(_.movingTime).toList.sum,
        ass.map(_.elapsedTime).toList.sum,
        ass.map(_.distance).toList.sum,
        ass.map(_.calories).toList.sum,
        sum(ass.map(_.totalAscend)),
        sum(ass.map(_.totalDescend)),
        ass.map(_.minTemp).toList.flatten.minOption,
        ass.map(_.maxTemp).toList.flatten.maxOption,
        sum(ass.map(_.avgTemp)).map(_ / len),
        ass.map(_.minHr).toList.flatten.minOption,
        ass.map(_.maxHr).toList.flatten.maxOption,
        sum(ass.map(_.avgHr)).map(_ / len),
        ass.map(_.maxSpeed).toList.flatten.maxOption,
        sum(ass.map(_.avgSpeed)).map(_ / len),
        ass.map(_.maxPower).toList.flatten.maxOption,
        sum(ass.map(_.avgPower)).map(_ / len),
        sum(ass.map(_.avgNormPower)).map(_ / len),
        sum(ass.map(_.avgCadence)).map(_ / len),
        sum(ass.map(_.avgGrade)).map(_ / len),
        sum(ass.map(_.avgIntensity)).map(_ / len),
        sum(ass.map(_.avgTss)).map(_ / len),
        ass.map(_.count).toList.sum
      )
    }

  given Numeric[java.time.Duration] = NumericFrom.javaDuration

  private def sum[A](data: NonEmptyList[Option[A]])(using
      numeric: Numeric[A]
  ): Option[A] = {
    given Semigroup[A] = Semigroup.instance(numeric.plus)

    data.foldLeft(Option.empty[A])(_ |+| _)
  }
}
