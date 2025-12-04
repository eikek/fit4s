package fit4s.core.data

import java.time.Instant

final case class Timespan(startTime: Instant, endTime: Instant):

  def contains(ts: Instant): Boolean =
    ts == startTime || ts == endTime ||
      (startTime.isBefore(ts) && endTime.isAfter(ts))

  def union(other: Timespan): Timespan =
    val s = if startTime.isBefore(other.startTime) then startTime else other.startTime
    val e = if endTime.isAfter(other.endTime) then endTime else other.endTime
    Timespan(s, e)

  def javaDuration: java.time.Duration =
    java.time.Duration.between(startTime, endTime)

  def duration: Duration =
    Duration.from(javaDuration)

  def isAll: Boolean = this == Timespan.all

object Timespan:

  val all: Timespan = Timespan(Instant.MIN, Instant.MAX)
