package fit4s.core.data

import fit4s.core.FieldReader
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
    def *(factor: Double): Speed = self * factor
    def /(div: Double): Speed = self / div
    def +(other: Speed): Speed = self + other
    def asString: String = f"${toKmh}%.2fkmh"
    private def ord: Ordered[Speed] =
      Ordered.orderingToOrdered(self)(using Ordering[Speed])
    export ord.*

  given Numeric[Speed] = Numeric.DoubleIsFractional
  given Ordering[Speed] = Ordering[Double]
  given FieldReader[Speed] =
    for
      _ <- FieldReader.unit(MeasurementUnit.MeterPerSecond)
      v <- FieldReader.firstAsDouble
    yield Speed.meterPerSecond(v)
  given Display[Speed] = Display.instance(_.asString)
