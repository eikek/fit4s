package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type HeartRate = Int

object HeartRate:
  val zero: HeartRate = 0

  def bpm(bpm: Int): HeartRate = bpm

  extension (self: HeartRate)
    def toBpm: Int = self
    def toString = s"${self}bpm"
    def +(hr: HeartRate): HeartRate = hr + self
    def /(d: Double): HeartRate = (self / d).toInt
    def asString = s"${self}bpm"

  given Numeric[HeartRate] = Numeric.IntIsIntegral
  given FieldReader[HeartRate] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Bpm)
      v <- FieldReader.firstAsInt
    yield v
  given Display[HeartRate] = Display.instance(_.asString)
