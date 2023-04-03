package fit4s

import fit4s.profile.messages.RecordMsg
import munit.CatsEffectSuite

import java.time.{Duration, Instant, ZoneId}

class ActivityReaderTest extends CatsEffectSuite {
  val zone: ZoneId = ZoneId.of("Europe/Berlin")

  test("read example activity") {
    for {
      data <- FitTestData.exampleActivity
      fit = FitFile.decodeUnsafe(data).head
      result = ActivityReader
        .read(fit, zone)
        .fold(err => sys.error(err.toString), identity)
      recCount = fit.dataRecords.filter(_.isMessage(RecordMsg))
      _ = assertEquals(result.activity.numSessions, 1)
      _ = assertEquals(result.sessions.size, 1)
      _ = assertEquals(result.unrelatedRecords.size, 0)
      _ = assertEquals(result.recordsFor(result.sessions.head).size, recCount.size)
    } yield ()
  }

  test("read cycling activity") {
    for {
      data <- FitTestData.edge530CyclingActivity
      fit = FitFile.decodeUnsafe(data).head
      result = ActivityReader
        .read(fit, zone)
        .fold(err => sys.error(err.toString), identity)
      recCount = fit.dataRecords.filter(_.isMessage(RecordMsg))
      _ = assertEquals(result.activity.numSessions, 1)
      _ = assertEquals(result.sessions.size, 1)
      _ = assertEquals(result.unrelatedRecords.size, 0)
      _ = assertEquals(result.recordsFor(result.sessions.head).size, recCount.size)
      _ = assertEquals(
        result.activity.timestamp.asInstant,
        Instant.parse("2023-03-16T06:29:27Z")
      )
      _ = assertEquals(
        result.activity.totalTime,
        Duration.ofSeconds(3678)
      )
    } yield ()
  }

  test("read Garmin Swim fit file") {
    for {
      data <- FitTestData.garminSwimActivity
      fit = FitFile.decodeUnsafe(data).head
      result = ActivityReader
        .read(fit, zone)
        .fold(err => sys.error(err.toString), identity)
      _ = assertEquals(
        result.activity.timestamp.asInstant,
        Instant.parse("2015-06-30T16:29:00Z")
      )
    } yield ()
  }
}
