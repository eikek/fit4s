package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type IntensityFactor = Double

object IntensityFactor:
  def iff(iff: Double): IntensityFactor = iff

  extension (self: IntensityFactor)
    def value: Double = self
    def asString = f"$self%.2fiff"

  given Numeric[IntensityFactor] = Numeric.DoubleIsFractional

  given FieldReader[IntensityFactor] =
    for
      _ <- FieldReader.unit(MeasurementUnit.IntensityFactor)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[IntensityFactor] = Display.instance(_.asString)
