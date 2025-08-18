package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Grade = Double

object Grade:
  def percent(p: Double): Grade = p

  extension (self: Grade)
    def toPercent: Double = self
    def asString = f"$self%.2f%%"

  given Numeric[Grade] = Numeric.DoubleIsFractional
  given FieldReader[Grade] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Percent)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[Grade] = Display.instance(_.asString)
