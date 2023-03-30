package fit4s.data

final class Semicircle(val semicircle: Long) extends AnyVal {
  def toDegree: Double =
    semicircle * (180d / (Int.MaxValue.toDouble + 1))

  def toSeconds: Long = semicircle * 20
}

object Semicircle {
  def semicircle(value: Long): Semicircle = new Semicircle(value)

  implicit val ordering: Ordering[Semicircle] =
    Ordering.by(_.semicircle)
}
