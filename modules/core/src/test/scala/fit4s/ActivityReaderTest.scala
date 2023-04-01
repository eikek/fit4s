package fit4s

import fit4s.profile.messages.RecordMsg
import munit.CatsEffectSuite

import java.time.{Duration, Instant}

class ActivityReaderTest extends CatsEffectSuite {

  test("read example activity") {
    for {
      data <- FitTestData.exampleActivity
      fit = FitFile.decodeUnsafe(data)
      result = ActivityReader.read(fit).fold(sys.error, identity)
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
      fit = FitFile.decodeUnsafe(data)
      result = ActivityReader.read(fit).fold(sys.error, identity)
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
}
