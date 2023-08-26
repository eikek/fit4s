package fit4s.cats.instances

import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant, ZoneId}
import java.util.Locale

import cats.Show
import cats.syntax.all._

import fit4s.cats.util.{DateInstant, TimeInstant}
import fit4s.data._
import fit4s.profile.types.{Sport, SubSport, SwimStroke}

trait ShowInstances {
  implicit def optionShow[A](implicit showA: Show[A]): Show[Option[A]] =
    Show.show {
      case None    => ""
      case Some(a) => showA.show(a)
    }

  implicit def tupleShow[A, B](implicit showA: Show[A], showB: Show[B]): Show[(A, B)] =
    Show.show(t => show"${t._1}|${t._2}")

  implicit def optionTupleShow[A, B](implicit
      showA: Show[A],
      showB: Show[B]
  ): Show[(Option[A], Option[B])] =
    Show.show {
      case (Some(a), Some(b)) => (a, b).show
      case (Some(a), None)    => a.show
      case (None, Some(b))    => b.show
      case (None, None)       => ""
    }

  implicit def sportSubSportShow: Show[(Sport, SubSport)] =
    Show.show {
      case (s, SubSport.Generic)                                     => s.show
      case (Sport.Generic, s)                                        => s.show
      case (a, b) if b.show.toLowerCase.contains(a.show.toLowerCase) => b.show
      case (a, b)                                                    => show"$a/$b"
    }

  implicit def speedShow(implicit sport: Sport): Show[Speed] =
    Show.show { speed =>
      if (speed.isZero) "-"
      else
        (speed, sport) match {
          case (sp, Sport.Swimming) => s"${minTomss(sp.minPer100m)} min/100m"
          case (sp, Sport.Running)  => s"${minTomss(sp.minPer1k)} min/km"
          case (sp, Sport.Walking)  => s"${minTomss(sp.minPer1k)} min/km"
          case (sp, Sport.Hiking)   => s"${minTomss(sp.minPer1k)} min/km"
          case (sp, _)              => sp.toString
        }
    }

  implicit val swimStrokeShow: Show[SwimStroke] =
    Show.show(_.typeName)

  private def minTomss(min: Double): String = {
    val minutes = min.floor.toInt
    val secs = (min - minutes) * 60
    f"$minutes:${secs.toInt}%02d"
  }

  implicit def instantShow(implicit zoneId: ZoneId): Show[Instant] =
    Show.show { i =>
      val zoned = i.atZone(zoneId)
      val dow = zoned.getDayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault)
      val ld = zoned.truncatedTo(ChronoUnit.MINUTES).toLocalDateTime
      s"$dow, ${ld.toLocalDate} ${ld.toLocalTime}"
    }

  implicit def dateInstantShow(implicit zoneId: ZoneId): Show[DateInstant] =
    Show.show { di =>
      val ld = di.instant.atZone(zoneId)
      ld.toLocalDate.toString
    }

  implicit def timeInstantShow(implicit zoneId: ZoneId): Show[TimeInstant] =
    Show.show { di =>
      val ld = di.instant.atZone(zoneId)
      ld.toLocalTime.toString
    }

  implicit val durationShow: Show[Duration] =
    Show.show { d =>
      val secs = d.toSeconds
      val hour = secs / 3600
      List(
        hour -> "h",
        (secs - (hour * 3600)) / 60 -> "min"
      ).filter(_._1 > 0)
        .map { case (v, u) => s"$v$u" }
        .mkString(" ")
    }

  implicit val distanceShow: Show[Distance] =
    Show.show { dst =>
      if (dst.km >= 1) f"${dst.km}%.1fkm" else f"${dst.meter.toInt}m"
    }

  implicit val temperatureShow: Show[Temperature] =
    Show.show { temp =>
      f"${temp.celcius}%.1f" match {
        case str if str.endsWith("0") => s"${temp.celcius.toInt}°C"
        case str                      => s"$str°C"
      }
    }

  implicit val intensityFactorShow: Show[IntensityFactor] =
    Show.show { iff =>
      f"${iff.iff}%.1fIF"
    }

  implicit val trainingStressScoreShow: Show[TrainingStressScore] =
    Show.show { tss =>
      f"${tss.tss}%.1ftss"
    }

  implicit val heartRateShow: Show[HeartRate] =
    Show.fromToString

  implicit val percentShow: Show[Percent] =
    Show.show { p =>
      s"${p.percent}%.1f%"
    }

  implicit val gradeShow: Show[Grade] =
    Show.show(g => s"${g.percent}%.1f%")

  implicit val caloriesShow: Show[Calories] =
    Show.show { cal =>
      s"${cal.kcal.toInt}kcal"
    }

  implicit val sportShow: Show[Sport] =
    Show.fromToString

  implicit val subSportShow: Show[SubSport] =
    Show.fromToString

  implicit val deviceProductShow: Show[DeviceProduct] =
    Show.show {
      case DeviceProduct.Garmin(d) => d.toString
      case DeviceProduct.Favero(d) => d.toString
      case DeviceProduct.Unknown   => "-"
    }

  implicit val powerShow: Show[Power] =
    Show.show { pwr =>
      s"${pwr.watts}W"
    }

  implicit val cadenceShow: Show[Cadence] =
    Show.show(c => s"${c.rpm}rpm")

  implicit val strokesPerLapShow: Show[StrokesPerLap] =
    Show.show(spl => s"${spl.spl} strokes/lap")

  implicit val semicircleShow: Show[Semicircle] =
    Show.show(sc => s"${sc.semicircle} semicircle")
}

object ShowInstances extends ShowInstances
