package fit4s.data

final class Power private (val watts: Int) extends AnyVal {

  def *(factor: Double): Power = new Power((watts * factor).toInt)

  def /(div: Double): Power = new Power((watts / div).toInt)

  def +(other: Power): Power = new Power(watts + other.watts)

  override def toString = s"${watts}watts"
}

object Power {
  val zero: Power = watts(0)

  def watts(watts: Int): Power = new Power(watts)

  implicit val ordering: Ordering[Power] =
    Ordering.by(_.watts)
}
