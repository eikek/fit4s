package fit4s.data

final class IntensityFactor(val iff: Double) extends AnyVal:
  def /(div: Double): IntensityFactor = new IntensityFactor(iff / div)
  override def toString = s"If($iff)"

object IntensityFactor:
  def iff(iff: Double): IntensityFactor = new IntensityFactor(iff)

  given Numeric[IntensityFactor] =
    NumericFrom[IntensityFactor, Double](_.iff, IntensityFactor.iff)
