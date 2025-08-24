package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Grade = Double

object Grade:
  val zero: Grade = 0
  val max: Grade = 100
  def percent(p: Double): Grade =
    if p > max then max else p

  extension (self: Grade)
    def toPercent: Double = self
    def asString = f"$self%.2f%%"
    private def ord: Ordered[Grade] =
      Ordered.orderingToOrdered(self)(using Ordering[Grade])
    export ord.*

  given Numeric[Grade] = Numeric.DoubleIsFractional
  given FieldReader[Grade] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Percent)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[Grade] = Display.instance(_.asString)
