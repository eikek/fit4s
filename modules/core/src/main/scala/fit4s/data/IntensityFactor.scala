package fit4s.data

final class IntensityFactor(val iff: Double) extends AnyVal {
  override def toString = s"If($iff)"
}

object IntensityFactor {
  def iff(iff: Double): IntensityFactor = new IntensityFactor(iff)

  implicit val ordering: Ordering[IntensityFactor] =
    Ordering.by[IntensityFactor, Double](_.iff)(Ordering.Double.TotalOrdering)
}
