package fit4s.data

final class Grade(val percent: Double) extends AnyVal

object Grade {
  def percent(p: Double): Grade = new Grade(p)

  implicit val ordering: Ordering[Grade] =
    Ordering.by[Grade, Double](_.percent)(Ordering.Double.TotalOrdering)
}
