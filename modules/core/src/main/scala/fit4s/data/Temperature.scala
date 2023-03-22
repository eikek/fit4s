package fit4s.data

final class Temperature private (val celcius: Double) extends AnyVal {
  def *(factor: Double): Temperature = new Temperature(celcius * factor)

  def /(div: Double): Temperature = new Temperature(celcius / div)

  def +(temp: Temperature): Temperature = new Temperature(celcius + temp.celcius)

  override def toString =
    if (this == Temperature.maxValue) "Max"
    else if (this == Temperature.minValue) "Min"
    else f"$celcius%2.2f°C"
}

object Temperature {
  val zero: Temperature = celcius(0)
  val maxValue: Temperature = celcius(Double.MaxValue)
  val minValue: Temperature = celcius(Double.MinValue)

  def celcius(celcius: Double): Temperature = new Temperature(celcius)

  implicit val ordering: Ordering[Temperature] =
    Ordering.by(_.celcius)
}
