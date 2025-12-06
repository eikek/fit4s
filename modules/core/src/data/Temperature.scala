package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type Temperature = Double

object Temperature:
  val zero: Temperature = 0
  def celcius(celcius: Double): Temperature = celcius

  extension (self: Temperature)
    def toCelcius: Double = self
    infix def +(t: Temperature): Temperature = self + t
    infix def *(f: Double): Temperature = self * f
    infix def /(d: Double): Temperature = self / d
    infix def -(t: Temperature): Temperature = self - t
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

  given FieldValueEncoder[Temperature] =
    FieldValueEncoder.forDouble.contramap(_.toCelcius)
