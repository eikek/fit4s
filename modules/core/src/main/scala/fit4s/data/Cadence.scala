package fit4s.data

final class Cadence(val rpm: Int) extends AnyVal {

  def /(div: Double): Cadence = new Cadence((rpm.toDouble / div).toInt)

  override def toString = s"Cadence($rpm)"
}

object Cadence {
  def rpm(rpm: Int): Cadence = new Cadence(rpm)

  given Numeric[Cadence] =
    NumericFrom[Cadence, Int](_.rpm, Cadence.rpm)
}
