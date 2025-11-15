package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Percent = Double

object Percent:
  val zero: Percent = 0
  val max: Percent = 100

  def percent(p: Double): Percent =
    if p > max then max else p

  extension (self: Percent)
    def value: Double = self

    infix def *(f: Double): Percent = self * f
    infix def /(d: Double): Percent = self / d
    infix def +(g: Percent): Percent = self + g
    infix def -(g: Percent): Percent = self - g

    def asString: String = f"$self%.2f%%"
    private def ord: Ordered[Percent] =
      Ordered.orderingToOrdered(self)(using Ordering[Percent])
    export ord.*

  given Fractional[Percent] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[Percent]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Percent)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[Percent] = reader.singleValue
  given Display[Percent] = Display.instance(_.asString)
