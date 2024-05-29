package fit4s.data

final class Semicircle(val semicircle: Long) extends AnyVal:
  def toDegree: Double =
    semicircle * Semicircle.scToDegFactor

  def toRadian =
    (semicircle * math.Pi) / Semicircle.maxC

  def toSeconds: Long = semicircle * 20

  override def toString = s"Semicircles($semicircle)"

object Semicircle:
  private val maxC = Int.MaxValue.toDouble + 1
  private val scToDegFactor = 180d / maxC

  def semicircle(value: Long): Semicircle = new Semicircle(value)

  def degree(deg: Double): Semicircle =
    semicircle((deg / scToDegFactor).toLong)

  given Numeric[Semicircle] =
    NumericFrom[Semicircle, Long](_.semicircle, Semicircle.semicircle)
