package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Distance = Double

object Distance:
  val zero: Distance = 0d

  def meter(meter: Double): Distance = meter
  def km(km: Double): Distance = km * 1000
  def millimeter(v: Double): Distance = v / 1000

  extension (self: Distance)
    def toMeter: Double = self
    def toKm: Double = self / 1000.0
    def *(factor: Double): Distance = self * factor
    def +(dst: Distance): Distance = self + dst
    def -(dst: Distance): Distance = self - dst
    def rounded: Distance = self.round.toDouble
    def roundTo(precision: Int): Distance =
      if precision <= 0 then rounded
      else
        val f = math.pow(10, precision)
        Math.round(self * f) / f

    def asString: String =
      if self > 1000 then f"$toKm%.2fkm"
      else f"$self%.2fm"

    private def ord: Ordered[Distance] =
      Ordered.orderingToOrdered(self)(using Ordering[Distance])
    export ord.*

  given Numeric[Distance] = Numeric.DoubleIsFractional
  given FieldReader[Distance] =
    for
      u <- FieldReader.unit(
        MeasurementUnit.Meter,
        MeasurementUnit.Km,
        MeasurementUnit.Millimeter
      )
      v <- FieldReader.firstAsDouble
      r = u match
        case MeasurementUnit.Meter =>
          Distance.meter(v)
        case MeasurementUnit.Km =>
          Distance.km(v)
        case _ =>
          Distance.millimeter(v)
    yield r
  given Display[Distance] = Display.instance(_.asString)
