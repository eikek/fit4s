package fit4s.data

import fit4s.profile.types._
import fit4s.{FitFile, FitTestData}
import munit.CatsEffectSuite

import java.time.Duration

class ActivitySessionTest extends CatsEffectSuite {

  test("read activity data") {
    for {
      raw <- FitTestData.examplePoolswimActivity
      fit = FitFile.decodeUnsafe(raw).head
      sess = fit.findFirstData(MesgNum.Session).fold(sys.error, identity)
      summary = ActivitySession.from(sess).fold(sys.error, identity)
      _ = assertEquals(
        summary,
        ActivitySession(
          sport = Sport.Swimming,
          subSport = SubSport.LapSwimming,
          startTime = DateTime(840028247L),
          endTime = DateTime(840031753L),
          movingTime = Some(Duration.parse("PT58M5S")),
          elapsedTime = Some(Duration.parse("PT58M24S")),
          calories = Calories.kcal(138.0),
          distance = Distance.meter(1700),
          minTemp = None,
          maxTemp = None,
          avgTemp = None,
          maxSpeed = Speed.meterPerSecond(1.262),
          avgSpeed = Some(Speed.meterPerSecond(0.965)),
          minHr = None,
          maxHr = Some(HeartRate.bpm(133)),
          avgHr = Some(HeartRate.bpm(103)),
          maxPower = None,
          avgPower = None,
          maxCadence = None,
          avgCadence = Some(Cadence.rpm(23)),
          totalAscend = None,
          totalDescend = None,
          startPosition = None
        )
      )
    } yield ()
  }

}
