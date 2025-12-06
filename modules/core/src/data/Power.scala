package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.core.FieldValueEncoder
import fit4s.profile.MeasurementUnit

opaque type Power = Int

object Power:
  val zero: Power = 0
  val max: Power = Int.MaxValue
  def watts(watts: Int): Power = watts

  extension (self: Power)
    def toWatts: Int = self
    infix def +(other: Power): Power = self + other
    infix def -(other: Power): Power = self - other
    infix def /(d: Double): Power = (self / d).toInt
    infix def *(f: Double): Power = (self * f).toInt
    def asString: String = s"${self}W"
    private def ord: Ordered[Power] =
      Ordered.orderingToOrdered(self)(using Ordering[Power])
    export ord.*

  given Integral[Power] = Numeric.IntIsIntegral
  given reader: FieldReader[Vector[Power]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Watt)
      v <- FieldReader.anyNumberInt
    yield v
  given FieldReader[Power] = reader.singleValue
  given Display[Power] = Display.instance(_.asString)

  given FieldValueEncoder[Power] =
    FieldValueEncoder.forInt.contramap(_.toWatts)
