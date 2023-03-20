package fit4s.data

final class HeartRate private (val bpm: Int) extends AnyVal {

  def *(factor: Double): HeartRate = new HeartRate((bpm * factor).toInt)

  def /(div: Double): HeartRate = new HeartRate((bpm / div).toInt)

  def +(other: HeartRate): HeartRate = new HeartRate(bpm + other.bpm)

  override def toString = s"${bpm}bpm"
}

object HeartRate {
  val zero: HeartRate = bpm(0)
  val minValue: HeartRate = bpm(Int.MinValue)
  val maxValue: HeartRate = bpm(Int.MaxValue)

  def bpm(bpm: Int): HeartRate = new HeartRate(bpm)

  implicit val ordering: Ordering[HeartRate] =
    Ordering.by(_.bpm)
}
