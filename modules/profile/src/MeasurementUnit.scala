package fit4s.profile

enum MeasurementUnit(val name: String, val alternativeNames: String*):

  case Millimeter extends MeasurementUnit("mm")
  case Meter extends MeasurementUnit("m")
  case Km extends MeasurementUnit("km")

  case Kg extends MeasurementUnit("kg")

  case Millisecond extends MeasurementUnit("ms")
  case Second extends MeasurementUnit("s")
  case Minute extends MeasurementUnit("minutes")
  case Hour extends MeasurementUnit("hr")
  case Year extends MeasurementUnit("years")

  case Bytes extends MeasurementUnit("bytes")
  case Steps extends MeasurementUnit("steps")
  case Bpm extends MeasurementUnit("bpm")
  case Percent extends MeasurementUnit("%", "percent")

  case MeterPerSecond extends MeasurementUnit("m/s")

  case Rpm extends MeasurementUnit("rpm")
  case Watt extends MeasurementUnit("watts")
  case KcalPerMin extends MeasurementUnit("kcal/min")
  case KgPerCubicMeter extends MeasurementUnit("kg/m^3")
  case Mps extends MeasurementUnit("mps")
  case Semicircles extends MeasurementUnit("semicircles")
  case Cycles extends MeasurementUnit("cycles")
  case Strides extends MeasurementUnit("strides")
  case Strokes extends MeasurementUnit("strokes")
  case Kcal extends MeasurementUnit("kcal")
  case StridesPerMinute extends MeasurementUnit("strides/min")
  case Lengths extends MeasurementUnit("lengths")
  case TrainingStressScore extends MeasurementUnit("tss")
  case IntensityFactor extends MeasurementUnit("if")
  case StrokesPerLap extends MeasurementUnit("strokes/lap")
  case SwimStroke extends MeasurementUnit("swim_stroke")
  case Joule extends MeasurementUnit("J")
  case Celcius extends MeasurementUnit("C")
  case Counts extends MeasurementUnit("counts")
  case GramPerDeciliter extends MeasurementUnit("g/dL")
  case Degree extends MeasurementUnit("degrees")
  case OxygenToxicityMeasurementUnit extends MeasurementUnit("OTUs")
  case BreathsPerMinute extends MeasurementUnit("breaths/min")
  case Grit extends MeasurementUnit("grit")
  case KGrit extends MeasurementUnit("kGrit")
  case Flow extends MeasurementUnit("flow")
  case Pascal extends MeasurementUnit("Pa")
  case BarPerMinute extends MeasurementUnit("bar/min")
  case LiterPerMinute extends MeasurementUnit("l/min")
  case V extends MeasurementUnit("V")
  case Calories extends MeasurementUnit("calories")
  case Gravity extends MeasurementUnit("G")
  case MGravity extends MeasurementUnit("mG")
  case DegreePerSecond extends MeasurementUnit("deg/s")
  case Radians extends MeasurementUnit("radians")
  case MeterPerSquareSecond extends MeasurementUnit("m/s^2")
  case RadiansPerSecond extends MeasurementUnit("radians/second")
  case KCalPerDay extends MeasurementUnit("kcal/day")
  case MmHg extends MeasurementUnit("mmHg")

  case Unknown(override val name: String) extends MeasurementUnit(name)

object MeasurementUnit:
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
      OxygenToxicityMeasurementUnit,
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

  private val unitMap: Map[String, MeasurementUnit] =
    all
      .flatMap(unit => (unit.name +: unit.alternativeNames).map(_.toLowerCase -> unit))
      .toMap

  def fromString(str: String): MeasurementUnit =
    val key = str.replace("\\s+", "").toLowerCase
    unitMap.getOrElse(key, Unknown(key))
