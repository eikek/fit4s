package fit4s.core
package data

import fit4s.profile.MeasurementUnit

opaque type IntensityFactor = Double

object IntensityFactor:
  def iff(iff: Double): IntensityFactor = iff

  /** Combines many intensity-factors into one by weighing each one with the duration. */
  def combine(fw: List[(Duration, IntensityFactor)]): Option[IntensityFactor] =
    if fw.isEmpty then None
    else
      val weighted = fw.map { case (d, iff) => (d, d.toSeconds * iff) }
      val (sumDur, sumIff) = weighted.tail.foldLeft(weighted.head) {
        case ((ad, aff), (d, iff)) =>
          (ad + d, aff + iff)
      }
      Some(sumIff / sumDur.toSeconds)

  extension (self: IntensityFactor)
    def value: Double = self
    def asString = f"$self%.2fiff"
    private def ord: Ordered[IntensityFactor] =
      Ordered.orderingToOrdered(self)(using Ordering[IntensityFactor])
    export ord.*

  given Fractional[IntensityFactor] = Numeric.DoubleIsFractional

  given reader: FieldReader[Vector[IntensityFactor]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.IntensityFactor)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[IntensityFactor] = reader.singleValue
  given Display[IntensityFactor] = Display.instance(_.asString)
