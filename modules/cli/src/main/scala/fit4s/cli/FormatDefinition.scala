package fit4s.cli

import cats.Show
import cats.syntax.all._
import fit4s.activities.data.ActivityId
import fit4s.cli.FormatDefinition.{DateInstant, TimeInstant}
import fit4s.data._
import fit4s.profile.types.{Sport, SubSport}

import java.time.{Duration, Instant, ZoneId}

trait FormatDefinition {

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
      case (s, SubSport.Generic) => s.show
      case (Sport.Generic, s)    => s.show
      case (a, b)                => show"$a/$b"
    }

  implicit def speedShow(implicit sport: Sport): Show[Speed] =
    Show.show { speed =>
      (speed, sport) match {
        case (sp, Sport.Swimming) => s"${minTomss(sp.minPer100m)} min/100m"
        case (sp, Sport.Running)  => s"${minTomss(sp.minPer1k)} min/km"
        case (sp, Sport.Walking)  => s"${minTomss(sp.minPer1k)} min/km"
        case (sp, Sport.Hiking)   => s"${minTomss(sp.minPer1k)} min/km"
        case (sp, _)              => sp.toString
      }
    }

  private def minTomss(min: Double): String = {
    val minutes = min.floor.toInt
    val secs = (min - minutes) * 60
    f"$minutes:${secs.toInt}%02d"
  }

  implicit def instantShow(implicit zoneId: ZoneId): Show[Instant] =
    Show.show { i =>
      val ld = i.atZone(zoneId).toLocalDateTime
      s"${ld.toLocalDate} ${ld.toLocalTime}"
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
    Show.fromToString

  implicit val heartRateShow: Show[HeartRate] =
    Show.fromToString

  implicit val iffShow: Show[IntensityFactor] =
    Show.show { iff =>
      f"${iff.iff}%.2fif"
    }

  implicit val tssShow: Show[TrainingStressScore] =
    Show.show { tss =>
      f"${tss.tss}%.2ftss"
    }

  implicit val percentShow: Show[Percent] =
    Show.show { p =>
      s"${p.percent}%.1f%"
    }

  implicit val caloriesShow: Show[Calories] =
    Show.show { cal =>
      s"${cal.kcal.toInt}kcal"
    }

  implicit val sportShow: Show[Sport] =
    Show.fromToString

  implicit val subSportShow: Show[SubSport] =
    Show.fromToString

  implicit val activityIdShow: Show[ActivityId] =
    Show.show(aid => f"${aid.id}% 4d")

  implicit val deviceProductShow: Show[DeviceProduct] =
    Show.show {
      case DeviceProduct.Garmin(d) => d.toString
      case DeviceProduct.Favero(d) => d.toString
      case DeviceProduct.Unknown   => "-"
    }

  implicit final class InstantOps(i: Instant) {
    def asDate: DateInstant = DateInstant(i)
    def asTime: TimeInstant = TimeInstant(i)
  }

  implicit class StringOps(self: String) {
    def in(s: Styles): String =
      s"${s.style}$self${Console.RESET}"
  }
}

object FormatDefinition extends FormatDefinition {

  case class DateInstant(instant: Instant)
  case class TimeInstant(instant: Instant)
}