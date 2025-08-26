package fit4s.core.data

import fit4s.codec.FitBaseValue
import fit4s.codec.FitBaseValue.syntax.*
import fit4s.core.FieldValue
import fit4s.core.data.Display.syntax.*
import fit4s.profile.DateTimeType
import fit4s.profile.MeasurementUnit

private[core] object FieldValueDisplay:
  val display: Display[FieldValue] = Display.instance { fv =>
    fieldValueShow(fv).mkString(", ")
  }

  private def enumShow(fv: FieldValue): Option[Vector[String]] =
    for
      pt <- fv.profileType
      n = fv.data.flatMap(_.asUInt)
      r = n.flatMap(pt.apply)
    yield r

  private def withUnitDisplay(
      fv: FieldValue,
      unit: MeasurementUnit
  ): Option[Vector[String]] =
    unit match
      case MeasurementUnit.Bpm =>
        fv.as[Vector[HeartRate]].map(_.map(_.display)).toOption
      case MeasurementUnit.Rpm =>
        fv.as[Vector[Cadence]].map(_.map(_.display)).toOption
      case MeasurementUnit.Kcal =>
        fv.as[Vector[Calories]].map(_.map(_.display)).toOption
      case MeasurementUnit.Calories =>
        fv.as[Vector[Calories]].map(_.map(_.display)).toOption
      case MeasurementUnit.Joule =>
        fv.as[Vector[Joule]].map(_.map(_.display)).toOption
      case MeasurementUnit.Meter =>
        fv.as[Vector[Distance]].map(_.map(_.display)).toOption
      case MeasurementUnit.Km =>
        fv.as[Vector[Distance]].map(_.map(_.display)).toOption
      case MeasurementUnit.Millimeter =>
        fv.as[Vector[Distance]].map(_.map(_.display)).toOption
      case MeasurementUnit.Percent =>
        fv.as[Vector[Percent]].map(_.map(_.display)).toOption
      case MeasurementUnit.IntensityFactor =>
        fv.as[Vector[IntensityFactor]].map(_.map(_.display)).toOption
      case MeasurementUnit.Watt =>
        fv.as[Vector[Power]].map(_.map(_.display)).toOption
      case MeasurementUnit.MeterPerSecond =>
        fv.as[Vector[Speed]].map(_.map(_.display)).toOption
      case MeasurementUnit.StrokesPerLap =>
        fv.as[Vector[StrokesPerLap]].map(_.map(_.display)).toOption
      case MeasurementUnit.Celcius =>
        fv.as[Vector[Temperature]].map(_.map(_.display)).toOption
      case MeasurementUnit.TrainingStressScore =>
        fv.as[Vector[TrainingStressScore]].map(_.map(_.display)).toOption
      case MeasurementUnit.Second =>
        if fv.profileType.contains(DateTimeType) then forDateTime(fv)
        else fv.as[Vector[Duration]].map(_.map(_.display)).toOption
      case _ => None

  private def forDateTime(fv: FieldValue) =
    fv.as[Vector[DateTime]].map(_.map(_.display)).toOption

  def fieldValueShow(fv: FieldValue): Vector[String] =
    fv.unit match
      case Some(u) =>
        withUnitDisplay(fv, u) match
          case Some(s) => s
          case None    =>
            forDateTime(fv).orElse(enumShow(fv)).getOrElse(fv.data.map(_.toString))
      case None =>
        forDateTime(fv).orElse(enumShow(fv)).getOrElse(fv.data.map(_.toString))
