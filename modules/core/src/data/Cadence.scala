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
    def /(div: Double): Cadence = (self.toDouble / div).toInt
    def +(c: Cadence): Cadence = self + c
    def -(c: Cadence): Cadence = self - c
    def asString = s"${self}rpm"

    private def ord: Ordered[Cadence] =
      Ordered.orderingToOrdered(self)(using Ordering[Cadence])
    export ord.*

  given Numeric[Cadence] = Numeric.IntIsIntegral
  given FieldReader[Cadence] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Rpm)
      v <- FieldReader.firstAsInt
    yield v

  given Display[Cadence] = Display.instance(_.asString)
