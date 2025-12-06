package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.core.FieldValueEncoder
import fit4s.profile.MeasurementUnit

opaque type Grade = Double

object Grade:
  val zero: Grade = 0
  val max: Grade = 100
  def percent(p: Double): Grade =
    if p > max then max else p

  extension (self: Grade)
    def toPercent: Double = self
    infix def *(f: Double): Grade = self * f
    infix def /(d: Double): Grade = self / d
    infix def +(g: Grade): Grade = self + g
    infix def -(g: Grade): Grade = self - g
    def asString = f"$self%.2f%%"
    private def ord: Ordered[Grade] =
      Ordered.orderingToOrdered(self)(using Ordering[Grade])
    export ord.*

  given Fractional[Grade] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[Grade]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Percent)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[Grade] = reader.singleValue

  given Display[Grade] = Display.instance(_.asString)

  given FieldValueEncoder[Grade] =
    FieldValueEncoder.forDouble.contramap(_.toPercent)
