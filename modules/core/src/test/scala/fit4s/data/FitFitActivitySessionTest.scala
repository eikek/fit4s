package fit4s.data

import java.time.Duration

import fit4s.profile.types._
import fit4s.{FitFile, FitTestData}

import munit.CatsEffectSuite

class FitFitActivitySessionTest extends CatsEffectSuite {

  test("read activity data") {
    for {
      raw <- FitTestData.examplePoolswimActivity
      fit = FitFile.decodeUnsafe(raw).head
      sess = fit.findFirstData(MesgNum.Session).fold(sys.error, identity)
      summary = FitActivitySession.from(sess).fold(sys.error, identity)
      _ = assertEquals(
        summary,
        FitActivitySession(
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
          normPower = None,
          maxCadence = None,
          avgCadence = Some(Cadence.rpm(23)),
          totalAscend = None,
          totalDescend = None,
          startPosition = None,
          trainingStressScore = None,
          numPoolLength = Some(68),
          intensityFactor = None,
          swimStroke = None,
          avgStrokeDistance = Some(Distance.meter(2.49)),
          avgStrokeCount = None,
          poolLength = Some(Distance.meter(25)),
          avgGrade = None
        )
      )
    } yield ()
  }

  // test("read indoor activity with gps invalid value") {
  //   for {
  //     raw <- FitTestData.indoorCyclingActivity
  //     fit = FitFile.decodeUnsafe(raw).head
  //     sess = fit.findFirstData(MesgNum.Session).fold(sys.error, identity)
  //     field = sess.dataFields.get(SessionMsg.startPositionLat).get
  //     lat = field.decodedValue.toEither.left
  //       .map(_.messageWithContext)
  //       .fold(sys.error, identity)
  //     _ = assertEquals(lat, FieldDecodeResult.InvalidValue(field.local))
  //   } yield ()
  // }
}
