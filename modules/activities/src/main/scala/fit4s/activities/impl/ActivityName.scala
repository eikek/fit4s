package fit4s.activities.impl

import java.time.{Instant, ZoneId}

import fit4s.profile.types.{DateTime, Sport}

object ActivityName {
  private val timeMap: Int => String = {
    case n if n >= 0 && n <= 4  => "Night"
    case n if n > 4 && n <= 11  => "Morning"
    case n if n > 11 && n <= 13 => "Lunch"
    case n if n > 13 && n <= 18 => "Afternoon"
    case n if n > 18 && n <= 21 => "Evening"
    case _                      => "Night"
  }

  def generate(startTime: Instant, sports: Set[Sport], zone: ZoneId): String = {
    val sport = sportVerb(sports)
    val hour = startTime.atZone(zone).getHour
    val timeVerb = timeMap(hour)
    s"$timeVerb $sport"
  }

  def generate(startTime: DateTime, sports: Set[Sport], zone: ZoneId): String =
    generate(startTime.asInstant, sports, zone)

  def sportVerb(sports: Set[Sport]): String =
    if (sports.isEmpty) "Activity"
    else if (sports.size > 1) "Multisport"
    else sportVerb(sports.head)

  def sportVerb(sport: Sport): String = sport match {
    case Sport.Cycling  => "Ride"
    case Sport.Running  => "Run"
    case Sport.Swimming => "Swim"
    case _              => sport.toString
  }
}
