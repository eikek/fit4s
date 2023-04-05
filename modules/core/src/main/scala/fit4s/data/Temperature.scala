package fit4s.data

final class Temperature private (val celcius: Double) extends AnyVal {
  def *(factor: Double): Temperature = new Temperature(celcius * factor)

  def /(div: Double): Temperature = new Temperature(celcius / div)

  def +(temp: Temperature): Temperature = new Temperature(celcius + temp.celcius)

  override def toString =
    f"$celcius%2.1f°C"
}

object Temperature {
  val zero: Temperature = celcius(0)

  def celcius(celcius: Double): Temperature = new Temperature(celcius)

  implicit val ordering: Ordering[Temperature] =
    Ordering.by(_.celcius)
}
