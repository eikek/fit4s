package fit4s.data

final class Grade(val percent: Double) extends AnyVal

object Grade {
  def percent(p: Double): Grade = new Grade(p)

  given Numeric[Grade] =
    NumericFrom[Grade, Double](_.percent, Grade.percent)
}
