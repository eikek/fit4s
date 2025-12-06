package fit4s.core.data

import fit4s.core.FieldReader
import fit4s.core.FieldValueEncoder
import fit4s.profile.MeasurementUnit

opaque type Semicircle = Long

object Semicircle:
  private val maxC = Int.MaxValue.toDouble + 1
  private val scToDegFactor = 180d / maxC

  val maxLat = degree(90)
  val maxLng = degree(180)
  val minLat = degree(-90)
  val minLng = degree(-180)

  def semicircle(value: Long): Semicircle = value

  def degree(deg: Double): Semicircle = (deg / scToDegFactor).toLong

  extension (self: Semicircle)
    def toSemicircle: Long = self
    def toDegree: Double = self * scToDegFactor
    def toRadian: Double = (self * math.Pi) / maxC
    def toSeconds: Long = self * 20
    def asString: String = s"${self}semicircle"
    infix def -(n: Semicircle): Semicircle = self - n
    infix def +(n: Semicircle): Semicircle = self + n
    infix def *(n: Double): Semicircle = (self * n).toLong
    infix def /(d: Double): Semicircle = (self / d).toLong

    private def ord: Ordered[Semicircle] =
      Ordered.orderingToOrdered(self)(using Ordering[Semicircle])
    export ord.*

  given Integral[Semicircle] = Numeric.LongIsIntegral

  given reader: FieldReader[Vector[Semicircle]] =
    for
      _ <- FieldReader.unit(MeasurementUnit.Semicircles)
      n <- FieldReader.longs
    yield n
  given FieldReader[Semicircle] = reader.singleValue
  given Display[Semicircle] = Display.instance(_.asString)

  given encoder: FieldValueEncoder[Semicircle] =
    FieldValueEncoder.instance { (field, sc) =>
      field.units.headOption match
        case Some(MeasurementUnit.Degree) =>
          FieldValueEncoder.forDouble.fitValue(field, sc.toDegree)
        case _ => FieldValueEncoder.forLong.fitValue(field, sc.toSemicircle)
    }
