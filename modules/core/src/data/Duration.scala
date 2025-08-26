package fit4s.core
package data

import java.text.DecimalFormat

import fit4s.profile.MeasurementUnit

opaque type Duration = Double

object Duration:
  private val fracFormat = DecimalFormat("#.000")
  val zero: Duration = 0
  def hours(n: Double): Duration = minutes(n * 60)
  def minutes(n: Double): Duration = secs(n * 60)
  def secs(s: Double): Duration = s
  def from(jd: java.time.Duration): Duration = jd.toSeconds().toDouble

  extension (self: Duration)
    def toSeconds: Double = self
    def asJava: java.time.Duration =
      java.time.Duration.ofSeconds(self.toLong)

    def +(t: Duration): Duration = self + t
    def *(f: Double): Duration = self * f
    def /(d: Double): Duration = self / d
    def asString: String =
      def split(n: Double, f: Int) =
        val k = (n / f).toInt
        (k, n - (k * f))

      val (hour, rem1) = split(self, 60 * 60)
      val (min, secs) = split(rem1, 60)
      val sfrac = secs - secs.toInt
      val str =
        if hour > 0 then f"$hour%02d:$min%02d:${secs.toInt}%02d"
        else if min > 0 then f"$min%02d:${secs.toInt}%02d"
        else f"${secs.toInt}%02d"
      if sfrac > 0 then f"$str${fracFormat.format(sfrac)}"
      else str

  given Numeric[Duration] = Numeric.DoubleIsFractional
  given FieldReader[Duration] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Second)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[Duration] = Display.instance(_.asString)
