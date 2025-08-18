package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Cadence = Int

object Cadence:
  def rpm(rpm: Int): Cadence = rpm

  extension (self: Cadence)
    def value: Int = self
    def /(div: Double): Cadence = (self.toDouble / div).toInt
    def asString = s"${self}rpm"

  given Numeric[Cadence] = Numeric.IntIsIntegral
  given FieldReader[Cadence] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Rpm)
      v <- FieldReader.firstAsInt
    yield v

  given Display[Cadence] = Display.instance(_.asString)
