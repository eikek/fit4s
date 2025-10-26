package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type Temperature = Double

object Temperature:
  val zero: Temperature = 0
  def celcius(celcius: Double): Temperature = celcius

  extension (self: Temperature)
    def toCelcius: Double = self
    def +(t: Temperature): Temperature = self + t
    def *(f: Double): Temperature = self * f
    def /(d: Double): Temperature = self / d
    def asString: String =
      if self == self.toInt then s"${self.toInt}°C"
      else f"$self%.2f°C"
    private def ord: Ordered[Temperature] =
      Ordered.orderingToOrdered(self)(using Ordering[Temperature])
    export ord.*

  given Fractional[Temperature] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[Temperature]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Celcius)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[Temperature] = reader.singleValue
  given Display[Temperature] = Display.instance(_.asString)
