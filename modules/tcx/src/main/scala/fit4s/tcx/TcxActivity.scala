package fit4s.tcx

import java.time.{Duration, Instant}
import fit4s.profile.types.*
import fit4s.data.*

import TcxActivity.Agg

final case class TcxActivity(
    id: Instant,
    sport: Sport,
    laps: Seq[TcxLap],
    creator: Option[TcxCreator]
) {

  lazy val totalTime =
    laps.map(_.totalTime).foldLeft(Duration.ZERO)(_ plus _)

  lazy val endTime: Instant = id.plus(totalTime)

  lazy val distance: Distance =
    Distance.meter(laps.map(_.distance.meter).sum)

  lazy val calories: Calories =
    Calories.kcal(laps.map(_.calories.kcal).sum)

  val (totalAscend, totalDescend) = {
    val alts = laps.flatMap(_.track).flatMap(_.altitude).map(_.meter)
    if (alts.isEmpty) (Distance.zero, Distance.zero)
    else {
      val (a, d) = alts.zip(alts.tail).foldLeft((0d, 0d)) { case ((up, down), (a, b)) =>
        val diff = b - a
        if (diff > 0) (up + diff, down)
        else (up, down + diff.abs)
      }
      (Distance.meter(a), Distance.meter(d))
    }
  }

  lazy val maxHr =
    laps.flatMap(_.maximumHeartRate).maxOption

  lazy val maxSpeed =
    laps.flatMap(_.maximumSpeed).maxOption

  lazy val maxCadence =
    laps.flatMap(_.cadence).maxOption

  lazy val startPosition: Option[Position] =
    laps.headOption.flatMap(_.track.headOption).flatMap(_.position)

  lazy val (avgHr, avgCadence) = {
    val empty = (Agg.zero, Agg.zero)
    val (aggHr, aggCad) = laps.flatMap(_.track).foldLeft(empty) { case ((hr, cad), el) =>
      (
        el.heartRate.map(hr.addHr).getOrElse(hr),
        el.cadence.map(cad.addCad).getOrElse(cad)
      )
    }

    (aggHr.avg.map(_.toInt).map(HeartRate.bpm), aggCad.avg.map(_.toInt).map(Cadence.rpm))
  }

  lazy val avgSpeed = {
    val s = laps.flatMap(_.avgSpeed)
    Option.when(s.nonEmpty)(
      Speed.meterPerSecond(s.map(_.meterPerSecond).sum / s.size.toDouble)
    )
  }

  lazy val deviceProduct: Option[DeviceProduct] =
    creator
      .flatMap(c => tryFindDevice(c.name))
      .map(DeviceProduct.Garmin.apply)

  lazy val fileId: FileId =
    FileId(
      fileType = File.Activity,
      manufacturer = Manufacturer.Garmin,
      product = deviceProduct.getOrElse(DeviceProduct.Unknown),
      serialNumber = creator.map(_.unitId),
      createdAt = Some(DateTime(Duration.between(DateTime.offset, id).toSeconds)),
      number = creator.map(_.productId),
      productName = creator.map(_.name)
    )

  private def tryFindDevice(name: String): Option[GarminProduct] = {
    val nameLc = name.toLowerCase()
    GarminProduct
      .byTypeName(name)
      .orElse(GarminProduct.all.find(p => p.typeName.startsWith(nameLc)))
  }
}

object TcxActivity {

  private case class Agg(value: Double, count: Int) {
    def add(v: Double): Agg = Agg(value + v, count + 1)
    def addHr(hr: HeartRate): Agg = add(hr.bpm.toDouble)
    def addCad(cad: Cadence): Agg = add(cad.rpm.toDouble)
    def avg: Option[Double] = Option.when(count > 0)(value / count.toDouble)
  }
  private object Agg {
    val zero: Agg = Agg(0, 0)
  }
}
