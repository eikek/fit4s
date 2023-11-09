package fit4s.data

final class Percent(val percent: Double) extends AnyVal {

  def /(div: Double): Percent = new Percent(percent / div)

  override def toString: String = s"${percent}%"
}

object Percent {
  def percent(p: Double): Percent = new Percent(p)

  given Numeric[Percent] =
    NumericFrom[Percent, Double](_.percent, Percent.percent)
}
