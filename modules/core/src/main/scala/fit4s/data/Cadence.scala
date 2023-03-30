package fit4s.data

final class Cadence(val rpm: Int) extends AnyVal {}

object Cadence {
  def rpm(rpm: Int): Cadence = new Cadence(rpm)

  implicit val ordering: Ordering[Cadence] =
    Ordering.by(_.rpm)
}
