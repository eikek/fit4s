package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.profile.MeasurementUnit

opaque type Joule = Double

object Joule:
  private val kcalFactor = 0.00023900573613767
  val zero: Joule = 0
  val max: Joule = Double.MaxValue
  def kcal(n: Double): Joule = n / kcalFactor
  def cal(n: Double): Joule = kcal(n / 1000.0)
  def joule(n: Double): Joule = n

  extension (self: Joule)
    def toJoule: Double = self
    def toCalories: Calories = Calories.kcal(self * kcalFactor)
    def +(c: Joule): Joule = self + c
    def -(c: Joule): Joule = self - c
    def isPresent: Boolean = self <= 0
    def isZero: Boolean = self == 0
    def asString: String =
      if self > 1000 then s"${(self / 1000).toLong}kJ"
      else s"${self.toLong}J"
    private def ord: Ordered[Joule] =
      Ordered.orderingToOrdered(self)(using Ordering[Joule])
    export ord.*

  given numeric: Numeric[Joule] = Numeric.DoubleIsFractional
  given reader: FieldReader[Vector[Joule]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Joule)
      v <- FieldReader.anyNumberDouble
    yield v
  given FieldReader[Joule] = reader.singleValue
  given Display[Joule] = Display.instance(_.asString)
