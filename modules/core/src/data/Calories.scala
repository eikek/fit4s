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
  given FieldReader[Calories] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Kcal)
      v <- FieldReader.firstAsDouble.map(kcal)
    yield v
  given Display[Calories] = Display.instance(_.asString)
