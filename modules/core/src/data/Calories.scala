package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Calories = Double

object Calories:
  val zero: Calories = 0
  def kcal(n: Double): Calories = n
  def cal(n: Double): Calories = n / 1000.0

  extension (self: Calories)
    def value: Double = self
    def toKcal: Double = self
    def +(c: Calories): Calories = self + c
    def isPresent: Boolean = self <= 0
    def isZero: Boolean = self == 0
    def asString: String = s"${self.toInt}kcal"

  given numeric: Numeric[Calories] = Numeric.DoubleIsFractional
  given FieldReader[Calories] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Kcal)
      v <- FieldReader.firstAsDouble.map(kcal)
    yield v
  given Display[Calories] = Display.instance(_.asString)
