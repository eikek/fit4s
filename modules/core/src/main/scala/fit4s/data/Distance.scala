package fit4s.data

final class Distance private (val meter: Double) extends AnyVal with Ordered[Distance] {

  def km: Double = meter / 1000.0

  def *(factor: Double): Distance = new Distance(meter * factor)

  def /(div: Double): Distance = new Distance(meter / div)

  def +(dst: Distance): Distance = new Distance(meter + dst.meter)

  def rounded: Distance = new Distance(meter.round.toDouble)

  def compare(that: Distance): Int = meter.compare(that.meter)

  override def toString = s"${meter}m"
}

object Distance {
  val zero: Distance = meter(0)

  def meter(meter: Double): Distance =
    new Distance(meter)

  def km(km: Double): Distance =
    new Distance(km * 1000)

  given Numeric[Distance] =
    NumericFrom[Distance, Double](_.meter, Distance.meter)
}
