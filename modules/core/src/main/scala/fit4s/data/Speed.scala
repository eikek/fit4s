package fit4s.data

final class Speed private (val meterPerSecond: Double) extends AnyVal {
  def isZero: Boolean = this == Speed.zero

  def kmh: Double = meterPerSecond * 3.6

  def minPer100m =
    if (meterPerSecond <= 0) 0
    else (100d / meterPerSecond) / 60d

  def minPer1k =
    if (meterPerSecond <= 0) 0
    else (1000d / meterPerSecond) / 60d

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

  given Numeric[Speed] =
    NumericFrom[Speed, Double](_.meterPerSecond, Speed.meterPerSecond)
}
