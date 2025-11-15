package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Distance = Double

object Distance:
  val zero: Distance = 0d
  val max: Distance = Double.MaxValue

  def meter(meter: Double): Distance = meter
  def km(km: Double): Distance = km * 1000
  def millimeter(v: Double): Distance = v / 1000

  extension (self: Distance)
    def toMeter: Double = self
    def toKm: Double = self / 1000.0
    infix def *(factor: Double): Distance = self * factor
    infix def /(div: Double): Distance = self / div
    infix def +(dst: Distance): Distance = self + dst
    infix def -(dst: Distance): Distance = self - dst
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

  given Fractional[Distance] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[Distance]] =
    for
      u <- FieldReader.unit(
        MeasurementUnit.Meter,
        MeasurementUnit.Km,
        MeasurementUnit.Millimeter
      )
      v <- FieldReader.anyNumberDouble
      r = u match
        case MeasurementUnit.Meter =>
          v.map(Distance.meter)
        case MeasurementUnit.Km =>
          v.map(Distance.km)
        case _ =>
          v.map(Distance.millimeter)
    yield r
  given FieldReader[Distance] = reader.singleValue
  given Display[Distance] = Display.instance(_.asString)
