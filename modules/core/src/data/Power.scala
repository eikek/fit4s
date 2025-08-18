package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Power = Int

object Power:
  val zero: Power = 0
  def watts(watts: Int): Power = watts

  extension (self: Power)
    def toWatts: Int = self
    def +(other: Power): Power = self + other
    def /(d: Double): Power = (self / d).toInt
    def asString: String = s"${self}W"

  given Numeric[Power] = Numeric.IntIsIntegral
  given FieldReader[Power] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Watt)
      v <- FieldReader.firstAsLong
    yield v.toInt
  given Display[Power] = Display.instance(_.asString)
