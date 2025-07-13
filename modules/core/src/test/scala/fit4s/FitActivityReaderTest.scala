package fit4s

import java.time.ZoneId

import fit4s.profile.messages.RecordMsg

import munit.CatsEffectSuite

class FitActivityReaderTest extends CatsEffectSuite:
  val zone: ZoneId = ZoneId.of("Europe/Berlin")

  test("read example activity"):
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
      _ = assertEquals(result.laps.values.map(_.size).sum, 1)
      _ = assertEquals(result.unrelatedLaps.size, 0)
      _ = assertEquals(result.recordsFor(result.sessions.head).size, recCount.size)
    } yield ()

  // test("read cycling activity") {
  //   for {
  //     data <- FitTestData.edge530CyclingActivity
  //     fit = FitFile.decodeUnsafe(data).head
  //     result = ActivityReader
  //       .read(fit, zone)
  //       .fold(err => sys.error(err.toString), identity)
  //     recCount = fit.dataRecords.filter(_.isMessage(RecordMsg))
  //     _ = assertEquals(result.activity.numSessions, 1)
  //     _ = assertEquals(result.sessions.size, 1)
  //     _ = assertEquals(result.unrelatedRecords.size, 0)
  //     _ = assertEquals(result.laps.values.map(_.size).sum, 6)
  //     _ = assertEquals(result.unrelatedLaps.size, 0)
  //     _ = assertEquals(result.recordsFor(result.sessions.head).size, recCount.size)
  //     _ = assertEquals(
  //       result.activity.timestamp.asInstant,
  //       Instant.parse("2023-03-16T06:29:27Z")
  //     )
  //     _ = assertEquals(
  //       result.activity.totalTime,
  //       Duration.ofSeconds(3678)
  //     )
  //   } yield ()
  // }

  // test("read cycling activity from edge 1040") {
  //   for {
  //     data <- FitTestData.edge1040CyclingActivity
  //     fit = FitFile.decodeUnsafe(data).head
  //     result = ActivityReader
  //       .read(fit, zone)
  //       .fold(err => sys.error(err.toString), identity)
  //     recMsgs = fit.dataRecords.filter(_.isMessage(RecordMsg))
  //     _ = assertEquals(recMsgs.size, 1225)
  //     _ = assertEquals(result.activity.numSessions, 1)
  //     _ = assertEquals(result.sessions.size, 1)
  //     _ = assertEquals(result.unrelatedRecords.size, 0)
  //     _ = assertEquals(result.laps.values.map(_.size).sum, 2)
  //     _ = assertEquals(result.unrelatedLaps.size, 0)
  //     _ = assertEquals(result.recordsFor(result.sessions.head).size, recMsgs.size)
  //     _ = assertEquals(
  //       result.activity.timestamp.asInstant,
  //       Instant.parse("2025-07-12T18:17:22Z")
  //     )
  //     _ = assertEquals(
  //       result.activity.totalTime,
  //       Duration.ofSeconds(1217)
  //     )
  //   } yield ()
  // }

  // test("read Garmin Swim fit file") {
  //   for {
  //     data <- FitTestData.garminSwimActivity
  //     fit = FitFile.decodeUnsafe(data).head
  //     result = ActivityReader
  //       .read(fit, zone)
  //       .fold(err => sys.error(err.toString), identity)
  //     _ = assertEquals(
  //       result.activity.timestamp.asInstant,
  //       Instant.parse("2015-06-30T16:29:00Z")
  //     )
  //     _ = assertEquals(
  //       result.sessions.head.maxSpeed,
  //       Speed.meterPerSecond(0)
  //     )
  //   } yield ()
  // }

  // test("fix missing values from records (1)") {
  //   for {
  //     data <- FitTestData.garminSwimActivity
  //     fit = FitFile.decodeUnsafe(data).head
  //     result = ActivityReader
  //       .read(fit, zone)
  //       .fold(err => sys.error(err.toString), identity)
  //     missing = result.copy(sessions = result.sessions.map(_.copy(elapsedTime = None)))
  //     fixed = ActivityReader.fixMissingValues(missing)
  //     _ = assertEquals(
  //       fixed.sessions.head.elapsedTime,
  //       Some(Duration.ofSeconds(22 * 60 + 55))
  //     )
  //   } yield ()
  // }

  // test("fix missing values from records (2)") {
  //   for {
  //     data <- FitTestData.edge530CyclingActivity
  //     fit = FitFile.decodeUnsafe(data).head
  //     result = ActivityReader
  //       .read(fit, zone)
  //       .fold(err => sys.error(err.toString), identity)
  //     _ = assertEquals(result.sessions.head.minHr, None)
  //     missing = result.copy(sessions = result.sessions.map(_.copy(elapsedTime = None)))
  //     fixed = ActivityReader.fixMissingValues(missing)
  //     _ = assertEquals(
  //       fixed.sessions.head.elapsedTime,
  //       Some(Duration.ofSeconds(61 * 60 + 20))
  //     )
  //     _ = assertEquals(
  //       fixed.sessions.head.minHr,
  //       Some(HeartRate.bpm(111))
  //     )
  //   } yield ()
  // }
