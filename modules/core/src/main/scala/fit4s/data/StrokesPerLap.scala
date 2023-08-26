package fit4s.data

final class StrokesPerLap(val spl: Double) extends AnyVal {
  override def toString = s"$spl strokes/lap"
}

object StrokesPerLap {
  def strokesPerLap(spl: Double): StrokesPerLap = new StrokesPerLap(spl)
  def spl(spl: Double) = strokesPerLap(spl)

  implicit val ordering: Ordering[StrokesPerLap] =
    Ordering.by[StrokesPerLap, Double](_.spl)(Ordering.Double.TotalOrdering)
}
