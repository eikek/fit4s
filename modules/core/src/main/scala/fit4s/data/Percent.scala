package fit4s.data

final class Percent(val percent: Double) extends AnyVal

object Percent {
  def percent(p: Double): Percent = new Percent(p)

  implicit val ordering: Ordering[Percent] =
    Ordering.by(_.percent)
}
