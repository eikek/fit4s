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
    infix def +(o: StrokesPerLap): StrokesPerLap = self + o
    infix def -(o: StrokesPerLap): StrokesPerLap = self - o
    infix def *(f: Double): StrokesPerLap = self * f
    infix def /(d: Double): StrokesPerLap = self / d
    private def ord: Ordered[StrokesPerLap] =
      Ordered.orderingToOrdered(self)(using Ordering[StrokesPerLap])
    export ord.*

  given Fractional[StrokesPerLap] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[StrokesPerLap]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.StrokesPerLap)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[StrokesPerLap] = reader.singleValue
  given Display[StrokesPerLap] = Display.instance(_.asString)

  given FieldValueEncoder[StrokesPerLap] =
    FieldValueEncoder.forDouble.contramap(_.value)
