package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.core.FieldValueEncoder
import fit4s.profile.MeasurementUnit

opaque type Speed = Double

object Speed:
  val zero: Speed = 0
  val max: Speed = Double.MaxValue

  def meterPerSecond(meterPerSecond: Double): Speed = meterPerSecond
  def kmh(kmh: Double): Speed = kmh / 3.6

  extension (self: Speed)
    def toKmh: Double = self * 3.6
    def toMeterPerSecond: Double = self
    def toMinutesPer100m: Double =
      if (self <= 0) 0 else (100d / self) / 60d
    def toMinutesPer1km: Double =
      if (self <= 0) 0 else (1000d / self) / 60d
    infix def *(factor: Double): Speed = self * factor
    infix def /(div: Double): Speed = self / div
    infix def +(other: Speed): Speed = self + other
    infix def -(other: Speed): Speed = self - other
    def asString: String = f"${toKmh}%.2fkmh"
    private def ord: Ordered[Speed] =
      Ordered.orderingToOrdered(self)(using Ordering[Speed])
    export ord.*

  given Fractional[Speed] = Numeric.DoubleIsFractional
  given Ordering[Speed] = Ordering[Double]
  given reader: FieldReader[Vector[Speed]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.MeterPerSecond)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[Speed] = reader.singleValue
  given Display[Speed] = Display.instance(_.asString)

  given FieldValueEncoder[Speed] =
    FieldValueEncoder.forDouble.contramap(_.toMeterPerSecond)
