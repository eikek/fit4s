package fit4s.data

final class HeartRate private (val bpm: Int) extends AnyVal:

  def *(factor: Double): HeartRate = new HeartRate((bpm * factor).toInt)

  def /(div: Double): HeartRate = new HeartRate((bpm / div).toInt)

  def +(other: HeartRate): HeartRate = new HeartRate(bpm + other.bpm)

  override def toString = s"${bpm}bpm"

object HeartRate:
  val zero: HeartRate = bpm(0)

  def bpm(bpm: Int): HeartRate = new HeartRate(bpm)

  given Numeric[HeartRate] =
    NumericFrom[HeartRate, Int](_.bpm, HeartRate.bpm)
