package fit4s.core.data

import fit4s.codec.FitBaseValue
import fit4s.codec.FitBaseValue.syntax.*
import fit4s.core.FieldValue
import fit4s.profile.MeasurementUnit

object FieldValueDisplay:
  given Display[FieldValue] = Display.instance { fv =>
    fieldValueShow(fv).mkString(", ")
  }

  def baseValueShow(value: FitBaseValue, unit: MeasurementUnit): Option[String] =
    unit match
      case MeasurementUnit.Bpm =>
        value.asInt.map(HeartRate.bpm).map(_.asString)
      case MeasurementUnit.Rpm =>
        value.asInt.map(Cadence.rpm).map(_.asString)
      case MeasurementUnit.Kcal =>
        value.asDouble.map(Calories.kcal).map(_.asString)
      case MeasurementUnit.Meter =>
        value.asDouble.map(Distance.meter).map(_.asString)
      case MeasurementUnit.Km =>
        value.asDouble.map(Distance.km).map(_.asString)
      case MeasurementUnit.Millimeter =>
        value.asDouble.map(Distance.millimeter).map(_.asString)
      case MeasurementUnit.Percent =>
        value.asDouble.map(Percent.percent).map(_.asString)
      case MeasurementUnit.IntensityFactor =>
        value.asDouble.map(IntensityFactor.iff).map(_.asString)
      case MeasurementUnit.Watt =>
        value.asInt.map(Power.watts).map(_.asString)
      case MeasurementUnit.MeterPerSecond =>
        value.asDouble.map(Speed.meterPerSecond).map(_.asString)
      case MeasurementUnit.StrokesPerLap =>
        value.asDouble.map(StrokesPerLap.strokesPerLap).map(_.asString)
      case MeasurementUnit.Celcius =>
        value.asDouble.map(Temperature.celcius).map(_.asString)
      case MeasurementUnit.TrainingStressScore =>
        value.asDouble.map(TrainingStressScore.tss).map(_.asString)
      case _ => None

  private def enumShow(fv: FieldValue): Option[String] =
    for
      pt <- fv.profileType
      n <- fv.data.headOption.flatMap(_.asUInt)
      r <- pt.values.get(n)
    yield r

  def fieldValueShow(fv: FieldValue): Vector[String] =
    fv.unit match
      case Some(u) =>
        fv.data.map(v => baseValueShow(v, u).getOrElse(v.toString))
      case None =>
        enumShow(fv) match
          case Some(str) => Vector(str)
          case None      => fv.data.map(_.toString)
