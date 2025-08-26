package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Calories = Double

object Calories:
  val zero: Calories = 0
  val max: Calories = Double.MaxValue
  def kcal(n: Double): Calories = n
  def cal(n: Double): Calories = n / 1000.0

  extension (self: Calories)
    def value: Double = self
    def toKcal: Double = self
    def +(c: Calories): Calories = self + c
    def -(c: Calories): Calories = self - c
    def isPresent: Boolean = self <= 0
    def isZero: Boolean = self == 0
    def asString: String = s"${self.toInt}kcal"
    private def ord: Ordered[Calories] =
      Ordered.orderingToOrdered(self)(using Ordering[Calories])
    export ord.*

  given numeric: Numeric[Calories] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[Calories]] =
    for
      unit <- FieldReader.unit(MeasurementUnit.Kcal, MeasurementUnit.Calories)
      v <- FieldReader.anyNumberDouble
      r = if unit == MeasurementUnit.Kcal then v.map(kcal) else v.map(cal)
    yield r
  given FieldReader[Calories] = reader.singleValue
  given Display[Calories] = Display.instance(_.asString)
