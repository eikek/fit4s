package fit4s.profile

import fit4s.data._
import fit4s.profile.messages.Msg
import fit4s.profile.types.BaseTypedValue.LongBaseValue
import fit4s.profile.types._
import fit4s.util._

import java.time.Duration

final case class FieldValue[A <: TypedValue[_]](
    field: Msg.FieldAttributes,
    value: A
) {

  def scaledValue: Option[Nel[Double]] =
    (value, field.scale) match {
      case (LongBaseValue(rv, _), List(scale)) =>
        Some(Nel.of(rv / scale - field.offset.getOrElse(0d)))

      case (ArrayFieldType.LongArray(list), List(scale)) =>
        Some(list.map(_ / scale - field.offset.getOrElse(0d)))

      case _ => None
    }

  private def numericSingleValue: Option[Either[Long, Double]] =
    scaledValue.map(_.head).map(_.asRight[Long]).orElse {
      value match {
        case LongBaseValue(n, _)            => Some(n.asLeft[Double])
        case ArrayFieldType.LongArray(list) => Some(list.head.asLeft[Double])
        case _                              => None
      }
    }

  def distance: Option[Distance] = {
    val value = numericSingleValue.map(_.fold(_.toDouble, identity))
    (value, field.unit) match {
      case (Some(v), Some(MeasurementUnit.Km)) => Some(Distance.km(v))
      case (v, Some(MeasurementUnit.Meter))    => v.map(Distance.meter)
      case _                                   => None
    }
  }

  def speed: Option[Speed] = {
    val value = numericSingleValue.map(_.fold(_.toDouble, identity))
    (value, field.unit) match {
      case (v, Some(MeasurementUnit.MeterPerSecond)) => v.map(Speed.meterPerSecond)
      case _                                         => None
    }
  }

  def temperature: Option[Temperature] =
    field.unit match {
      case Some(MeasurementUnit.Celcius) =>
        numericSingleValue.map(_.fold(_.toDouble, identity)).map(Temperature.celcius)
      case _ => None
    }

  def heartrate: Option[HeartRate] =
    field.unit match {
      case Some(MeasurementUnit.Bpm) =>
        numericSingleValue.map(_.fold(_.toInt, _.toInt)).map(HeartRate.bpm)
      case _ => None
    }

  def duration: Option[Duration] = {
    val value = numericSingleValue.map(_.fold(identity, _.toLong))
    field.unit match {
      case Some(MeasurementUnit.Millisecond) => value.map(Duration.ofMillis)
      case Some(MeasurementUnit.Second)      => value.map(Duration.ofSeconds)
      case Some(MeasurementUnit.Minute)      => value.map(Duration.ofMinutes)
      case Some(MeasurementUnit.Hour)        => value.map(Duration.ofHours)
      case _                                 => None
    }
  }

  def calories: Option[Calories] = {
    val value = numericSingleValue.map(_.fold(_.toDouble, identity))
    field.unit match {
      case Some(MeasurementUnit.Calories) => value.map(Calories.cal)
      case Some(MeasurementUnit.Kcal)     => value.map(Calories.kcal)
      case _                              => None
    }
  }

  def valueAsString = {
    val amount = scaledValue
      .map {
        case Nel(h, Nil) => h.toString
        case l           => l.toList.toString
      }
      .getOrElse(value match {
        case LongBaseValue(rv, _)   => rv.toString
        case ArrayFieldType(nel, _) => nel.toList.toString
        case dt: DateTime           => dt.asInstant.toString
        case dt: LocalDateTime      => dt.asLocalDateTime.toString
        case _                      => value.toString
      })
    val unit = field.unit.map(_.name).getOrElse("")
    s"$amount$unit"
  }

  override def toString: String =
    s"${field.fieldName}=$valueAsString"
}
