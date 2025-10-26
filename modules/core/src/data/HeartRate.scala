package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type HeartRate = Int

object HeartRate:
  val zero: HeartRate = 0
  val max: HeartRate = 300

  def bpm(bpm: Int): HeartRate =
    if bpm > max then max else bpm

  extension (self: HeartRate)
    def toBpm: Int = self
    def toString = s"${self}bpm"
    def +(hr: HeartRate): HeartRate = hr + self
    def /(d: Double): HeartRate = (self / d).toInt
    def asString = s"${self}bpm"

  given Integral[HeartRate] = Numeric.IntIsIntegral
  given reader: FieldReader[Vector[HeartRate]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Bpm)
      v <- FieldReader.anyNumberInt
    yield v
  given FieldReader[HeartRate] = reader.singleValue
  given Display[HeartRate] = Display.instance(_.asString)
