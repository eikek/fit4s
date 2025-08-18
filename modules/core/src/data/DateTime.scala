package fit4s.core
package data

import java.time.Instant

import fit4s.codec.FitBaseType
import fit4s.profile.DateTimeType

/** if date_time is < 0x10000000 then it is system time (seconds from device power on) */
final case class DateTime(value: Long):
  val baseType = FitBaseType.byName(DateTimeType.baseType).get

  def asInstant: Instant =
    DateTime.offset.plusSeconds(value)

  def isSystemTime: Boolean =
    value < DateTime.minTimeForOffset

  def asString = asInstant.toString

object DateTime:
  given FieldReader[DateTime] = FieldReader.firstAsLong.map(DateTime.apply)

  val minTimeForOffset: Long = 0x10000000L
  val offset: Instant = Instant.parse("1989-12-31T00:00:00Z")

  def seconds(n: Long): DateTime = DateTime(n)
  def fromInstant(i: Instant): DateTime =
    DateTime(i.getEpochSecond - offset.getEpochSecond)

  given Display[DateTime] = Display.instance(_.asString)
