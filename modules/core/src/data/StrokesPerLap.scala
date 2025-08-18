package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type StrokesPerLap = Double

object StrokesPerLap:
  def strokesPerLap(spl: Double): StrokesPerLap = spl
  def spl(spl: Double): StrokesPerLap = spl

  extension (self: StrokesPerLap)
    def value: Double = self
    def asString: String = s"$self strokes/lap"

  given Numeric[StrokesPerLap] = Numeric.DoubleIsFractional
  given FieldReader[StrokesPerLap] =
    for
      _ <- FieldReader.unit(MeasurementUnit.StrokesPerLap)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[StrokesPerLap] = Display.instance(_.asString)
