package fit4s.profile

import fit4s.data._
import fit4s.profile.messages.Msg
import fit4s.profile.types._
import fit4s.util._

import java.time.Duration

final case class FieldValue[A <: GenFieldType](
    field: Msg.FieldAttributes,
    value: A
) {

  private def scaledValue: Option[Nel[Double]] =
    (value, field.scale) match {
      case (LongFieldType(rv, _), List(scale)) =>
        Some(Nel.of(rv / scale))

      case (ArrayFieldType.LongArray(list), List(scale)) =>
        Some(list.map(_ / scale))

      case _ => None
    }

  private def numericSingleValue: Option[Either[Long, Double]] =
    scaledValue.map(_.head).map(_.asRight[Long]).orElse {
      value match {
        case LongFieldType(n, _)            => Some(n.asLeft[Double])
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

  override def toString: String = {
    val amount = scaledValue
      .map {
        case Nel(h, Nil) => h.toString
        case l           => l.toString
      }
      .getOrElse(value match {
        case LongFieldType(rv, _) => rv.toString
        case dt: DateTime         => dt.asInstant.toString
        case _                    => value.toString
      })
    val unit = field.unit.map(_.name).getOrElse("")
    s"${field.fieldName}=$amount$unit"
  }
}
