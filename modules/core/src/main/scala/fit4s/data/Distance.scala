package fit4s.data

final class Distance private (val meter: Double) extends AnyVal {

  def km: Double = meter / 1000.0

  def *(factor: Double): Distance = new Distance(meter * factor)

  def /(div: Double): Distance = new Distance(meter / div)

  def +(dst: Distance): Distance = new Distance(meter + dst.meter)

  override def toString = s"${meter}m"
}

object Distance {
  val zero: Distance = meter(0)

  def meter(meter: Double): Distance =
    new Distance(meter)

  def km(km: Double): Distance =
    new Distance(km * 1000)

  implicit val ordering: Ordering[Distance] =
    Ordering.by(_.meter)
}
