package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type IntensityFactor = Double

object IntensityFactor:
  def iff(iff: Double): IntensityFactor = iff

  extension (self: IntensityFactor)
    def value: Double = self
    def asString = f"$self%.2fiff"
    infix def +(n: IntensityFactor): IntensityFactor = self + n
    infix def -(n: IntensityFactor): IntensityFactor = self - n
    infix def *(f: Double): IntensityFactor = self * f
    infix def /(d: Double): IntensityFactor = self / d

    private def ord: Ordered[IntensityFactor] =
      Ordered.orderingToOrdered(self)(using Ordering[IntensityFactor])
    export ord.*

  given Fractional[IntensityFactor] = Numeric.DoubleIsFractional

  given reader: FieldReader[Vector[IntensityFactor]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.IntensityFactor)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[IntensityFactor] = reader.singleValue
  given Display[IntensityFactor] = Display.instance(_.asString)
