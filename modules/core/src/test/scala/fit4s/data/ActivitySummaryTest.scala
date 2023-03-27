package fit4s.data

import fit4s.profile.types._
import fit4s.{FitFile, TestData}
import munit.CatsEffectSuite

import java.time.{Duration, Instant}

class ActivitySummaryTest extends CatsEffectSuite {

  test("read activity data") {
    for {
      raw <- TestData.examplePoolswimActivity
      fit = FitFile.decodeUnsafe(raw)
      summary = ActivitySummary.from(fit).fold(sys.error, identity)
      _ = assertEquals(
        summary,
        ActivitySummary(
          id = FileId(
            File.Activity,
            Manufacturer.Garmin,
            DeviceProduct.Garmin(GarminProduct.Fenix3),
            None,
            Some(DateTime(840028249)),
            None,
            None
          ),
          sport = Sport.Swimming,
          subSport = SubSport.LapSwimming,
          startTime = Instant.parse("2016-08-13T13:10:47Z"),
          movingTime = Duration.ZERO,
          elapsedTime = Duration.parse("PT58M24S"),
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
          avgPower = None
        )
      )
    } yield ()
  }

}
