package fit4s.profile.types

sealed trait MeasurementUnit {
  def name: String

  def alternativeNames: List[String] = Nil
}

object MeasurementUnit {

  case object Km extends MeasurementUnit { val name = "km" }
  case object Kg extends MeasurementUnit { val name = "kg" }
  case object Second extends MeasurementUnit { val name = "s" }
  case object Minute extends MeasurementUnit { val name = "minutes" }
  case object Hour extends MeasurementUnit { val name = "hr" }
  case object Millisecond extends MeasurementUnit { val name = "ms" }
  case object Year extends MeasurementUnit { val name = "years" }
  case object Bytes extends MeasurementUnit { val name = "bytes" }
  case object Steps extends MeasurementUnit { val name = "steps" }
  case object Meter extends MeasurementUnit { val name = "m" }
  case object Bpm extends MeasurementUnit { val name = "bpm" }
  case object Percent extends MeasurementUnit {
    val name = "%"
    override val alternativeNames = List("percent")
  }
  case object Millimeter extends MeasurementUnit { val name = "mm" }
  case object MeterPerSecond extends MeasurementUnit { val name = "m/s" }
  case object Rpm extends MeasurementUnit { val name = "rpm" }
  case object Watt extends MeasurementUnit { val name = "watts" }
  case object KcalPerMin extends MeasurementUnit { val name = "kcal/min" }
  case object KgPerCubicMeter extends MeasurementUnit { val name = "kg/m^3" }
  case object Mps extends MeasurementUnit { val name = "mps" }
  case object Semicircles extends MeasurementUnit { val name = "semicircles" }
  case object Cycles extends MeasurementUnit { val name = "cycles" }
  case object Strides extends MeasurementUnit { val name = "strides" }
  case object Strokes extends MeasurementUnit { val name = "strokes" }
  case object Kcal extends MeasurementUnit { val name = "kcal" }
  case object StridesPerMinute extends MeasurementUnit { val name = "strides/min" }
  case object Lengths extends MeasurementUnit { val name = "lengths" }
  case object TrainingStressScore extends MeasurementUnit { val name = "tss" }
  case object IntensityFactor extends MeasurementUnit { val name = "if" }
  case object StrokesPerLap extends MeasurementUnit { val name = "strokes/lap" }
  case object SwimStroke extends MeasurementUnit { val name = "swim_stroke" }
  case object Joule extends MeasurementUnit { val name = "J" }
  case object Celcius extends MeasurementUnit { val name = "C" }
  case object Counts extends MeasurementUnit { val name = "counts" }
  case object GramPerDeciliter extends MeasurementUnit { val name = "g/dL" }
  case object Degree extends MeasurementUnit { val name = "degrees" }
  case object OxygenToxicityMeasurementUnit$ extends MeasurementUnit { val name = "OTUs" }
  case object BreathsPerMinute extends MeasurementUnit { val name = "breaths/min" }
  case object Grit extends MeasurementUnit { val name = "grit" }
  case object KGrit extends MeasurementUnit { val name = "kGrit" }
  case object Flow extends MeasurementUnit { val name = "flow" }
  case object Pascal extends MeasurementUnit { val name = "Pa" }
  case object BarPerMinute extends MeasurementUnit { val name = "bar/min" }
  case object LiterPerMinute extends MeasurementUnit { val name = "l/min" }
  case object V extends MeasurementUnit { val name = "V" }
  case object Calories extends MeasurementUnit { val name = "calories" }
  case object Gravity extends MeasurementUnit { val name = "G" }
  case object MGravity extends MeasurementUnit { val name = "mG" }
  case object DegreePerSecond extends MeasurementUnit { val name = "deg/s" }
  case object Radians extends MeasurementUnit { val name = "radians" }
  case object MeterPerSquareSecond extends MeasurementUnit { val name = "m/s^2" }
  case object RadiansPerSecond extends MeasurementUnit { val name = "radians/second" }
  case object KCalPerDay extends MeasurementUnit { val name = "kcal/day" }
  case object MmHg extends MeasurementUnit { val name = "mmHg" }

  case class Unknown(name: String) extends MeasurementUnit

  val all: List[MeasurementUnit] =
    List(
      Km,
      Kg,
      Second,
      Minute,
      Hour,
      Millisecond,
      Year,
      Bytes,
      Steps,
      Meter,
      Bpm,
      Percent,
      Millimeter,
      MeterPerSecond,
      Rpm,
      Watt,
      KcalPerMin,
      KgPerCubicMeter,
      Mps,
      Semicircles,
      Cycles,
      Strides,
      Strokes,
      Kcal,
      StridesPerMinute,
      Lengths,
      TrainingStressScore,
      IntensityFactor,
      StrokesPerLap,
      SwimStroke,
      Joule,
      Celcius,
      Counts,
      GramPerDeciliter,
      Degree,
      OxygenToxicityMeasurementUnit$,
      BreathsPerMinute,
      Grit,
      KGrit,
      Flow,
      Pascal,
      BarPerMinute,
      LiterPerMinute,
      V,
      Calories,
      Gravity,
      MGravity,
      DegreePerSecond,
      Radians,
      MeterPerSquareSecond,
      RadiansPerSecond,
      KCalPerDay,
      MmHg
    )

  private[this] val unitMap: Map[String, MeasurementUnit] =
    all
      .flatMap(unit => (unit.name :: unit.alternativeNames).map(_.toLowerCase -> unit))
      .toMap

  def fromString(str: String): MeasurementUnit = {
    val key = str.replace("\\s+", "").toLowerCase
    unitMap.getOrElse(key, Unknown(key))
  }
}
