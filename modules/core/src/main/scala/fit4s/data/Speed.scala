package fit4s.data

final class Speed private (val meterPerSecond: Double) extends AnyVal {

  def kmh: Double = meterPerSecond * 3.6

  def minPer100m = (100d / meterPerSecond) / 60d

  def minPer1k = (1000d / meterPerSecond) / 60d

  def *(factor: Double): Speed = new Speed(meterPerSecond * factor)

  def /(div: Double): Speed = new Speed(meterPerSecond / div)

  def +(spd: Speed): Speed = new Speed(meterPerSecond + spd.meterPerSecond)

  override def toString = f"$kmh%.2fkmh"
}

object Speed {
  val zero: Speed = meterPerSecond(0)

  def meterPerSecond(meterPerSecond: Double): Speed =
    new Speed(meterPerSecond)

  def kmh(kmh: Double): Speed =
    meterPerSecond(kmh / 3.6)

  implicit val ordering: Ordering[Speed] =
    Ordering.by(_.meterPerSecond)
}
