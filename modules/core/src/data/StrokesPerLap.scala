package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type StrokesPerLap = Double

object StrokesPerLap:
  val zero: StrokesPerLap = 0
  val max: StrokesPerLap = Double.MaxValue

  def strokesPerLap(spl: Double): StrokesPerLap = spl
  def spl(spl: Double): StrokesPerLap = spl

  extension (self: StrokesPerLap)
    def value: Double = self
    def asString: String = s"$self strokes/lap"
    private def ord: Ordered[StrokesPerLap] =
      Ordered.orderingToOrdered(self)(using Ordering[StrokesPerLap])
    export ord.*

  given Numeric[StrokesPerLap] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[StrokesPerLap]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.StrokesPerLap)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[StrokesPerLap] = reader.singleValue
  given Display[StrokesPerLap] = Display.instance(_.asString)
