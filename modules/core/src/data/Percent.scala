package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Percent = Double

object Percent:
  def percent(p: Double): Percent = p

  extension (self: Percent)
    def value: Double = self
    def /(d: Double): Percent = self / d
    def asString: String = f"$self%.2f%%"

  given Numeric[Percent] = Numeric.DoubleIsFractional
  given FieldReader[Percent] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Percent)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[Percent] = Display.instance(_.asString)
