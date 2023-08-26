package fit4s.data

final class Cadence(val rpm: Int) extends AnyVal {
  override def toString = s"Cadence($rpm)"
}

object Cadence {
  def rpm(rpm: Int): Cadence = new Cadence(rpm)

  implicit val ordering: Ordering[Cadence] =
    Ordering.by(_.rpm)
}
