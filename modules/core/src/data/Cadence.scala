package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Cadence = Int

object Cadence:
  val zero: Cadence = 0
  val max: Cadence = Int.MaxValue

  def rpm(rpm: Int): Cadence = rpm

  extension (self: Cadence)
    def value: Int = self
    infix def *(f: Double): Cadence = (self * f).toInt
    infix def /(div: Double): Cadence = (self.toDouble / div).toInt
    infix def +(c: Cadence): Cadence = self + c
    infix def -(c: Cadence): Cadence = self - c
    def asString = self.toString

    private def ord: Ordered[Cadence] =
      Ordered.orderingToOrdered(self)(using Ordering[Cadence])
    export ord.*

  given Integral[Cadence] = Numeric.IntIsIntegral
  given reader: FieldReader[Vector[Cadence]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Rpm, MeasurementUnit.StridesPerMinute)
      v <- FieldReader.anyNumberInt
    yield v
  given FieldReader[Cadence] = reader.singleValue

  given Display[Cadence] = Display.instance(_.asString)
