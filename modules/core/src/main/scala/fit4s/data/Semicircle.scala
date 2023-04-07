package fit4s.data

final class Semicircle(val semicircle: Long) extends AnyVal {
  def toDegree: Double =
    semicircle * Semicircle.scToDegFactor

  def toSeconds: Long = semicircle * 20

  override def toString = s"Semicircles($semicircle)"
}

object Semicircle {
  private val scToDegFactor = 180d / (Int.MaxValue.toDouble + 1)

  def semicircle(value: Long): Semicircle = new Semicircle(value)

  def degree(deg: Double): Semicircle =
    semicircle((deg / scToDegFactor).toLong)

  implicit val ordering: Ordering[Semicircle] =
    Ordering.by(_.semicircle)
}
