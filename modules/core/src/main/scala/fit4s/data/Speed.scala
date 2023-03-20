package fit4s.data

final class Speed private (val meterPerSecond: Double) extends AnyVal {

  def kmh: Double = meterPerSecond * 3.6

  def *(factor: Double): Speed = new Speed(meterPerSecond * factor)

  def /(div: Double): Speed = new Speed(meterPerSecond / div)

  def +(spd: Speed): Speed = new Speed(meterPerSecond + spd.meterPerSecond)

  override def toString =
    f"$kmh%.2fkmh"
}

object Speed {
  val zero: Speed = meterPerSecond(0)
  val maxValue: Speed = meterPerSecond(Double.MaxValue)
  val minValue: Speed = meterPerSecond(Double.MinValue)

  def meterPerSecond(meterPerSecond: Double): Speed =
    new Speed(meterPerSecond)

  implicit val ordering: Ordering[Speed] =
    Ordering.by(_.meterPerSecond)
}
