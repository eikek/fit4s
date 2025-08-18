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
    def asString: String = f"$self%.2f°C"

  given Numeric[Temperature] = Numeric.DoubleIsFractional
  given FieldReader[Temperature] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Celcius)
      v <- FieldReader.firstAsDouble
    yield v
  given Display[Temperature] = Display.instance(_.asString)
