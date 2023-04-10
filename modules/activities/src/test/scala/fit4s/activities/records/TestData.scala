package fit4s.activities.records

import fit4s.activities.data.{ActivityId, ActivityLapId, ActivitySessionId, LocationId}
import fit4s.data._
import fit4s.profile.types.{
  DateTime,
  File,
  GarminProduct,
  LapTrigger,
  Manufacturer,
  Sport,
  SubSport,
  SwimStroke
}

import java.time.{Duration, Instant}

trait TestData {
  val importDate: Instant = Instant.parse("2023-04-07T11:00:11Z")

  val testActivity = RActivity(
    id = ActivityId(-1),
    locationId = LocationId(-1),
    path = "storage/file.fit",
    activityFileId = FileId(
      fileType = File.Activity,
      manufacturer = Manufacturer.Garmin,
      product = DeviceProduct.Garmin(GarminProduct.Fenix5),
      serialNumber = Some(1236549),
      createdAt = Some(DateTime(8944564)),
      number = None,
      productName = None
    ),
    name = "Morning Ride",
    timestamp = Instant.parse("2023-03-22T21:22:34Z"),
    totalTime = Duration.ofMinutes(56),
    notes = Some("this is a note"),
    importDate = importDate
  )

  val testActivitySession = RActivitySession(
    id = ActivitySessionId(-1),
    activityId = ActivityId(-1),
    sport = Sport.Cycling,
    subSport = SubSport.Generic,
    startTime = Instant.parse("2023-03-22T21:22:34Z"),
    endTime = Instant.parse("2023-03-22T22:21:34Z"),
    movingTime = Duration.ofSeconds(5640),
    elapsedTime = Duration.ofSeconds(6670),
    distance = Distance.km(36.1),
    startPosition =
      Option(Position(Semicircle.semicircle(154655L), Semicircle.semicircle(9944546L))),
    calories = Calories.kcal(246),
    totalAscend = Some(Distance.meter(351)),
    totalDescend = Some(Distance.meter(355)),
    minTemp = Some(Temperature.celcius(-2)),
    maxTemp = Some(Temperature.celcius(4)),
    avgTemp = Some(Temperature.celcius(1.6)),
    minHr = Some(HeartRate.bpm(118)),
    maxHr = Some(HeartRate.bpm(190)),
    avgHr = Some(HeartRate.bpm(161)),
    maxSpeed = Some(Speed.kmh(56.9)),
    avgSpeed = Some(Speed.kmh(21.4)),
    maxPower = None,
    avgPower = Some(Power.watts(211)),
    normPower = Some(Power.watts(198)),
    tss = Some(TrainingStressScore.tss(2.4)),
    numPoolLength = Some(68),
    iff = Some(IntensityFactor.iff(3.1)),
    swimStroke = Some(SwimStroke.Freestyle),
    avgStrokeDistance = Some(Distance.meter(2.4)),
    avgStrokeCount = Some(StrokesPerLap.strokesPerLap(34.4)),
    poolLength = Some(Distance.meter(50)),
    avgGrade = Some(Percent.percent(6.4)),
    maxCadence = Some(Cadence.rpm(121)),
    avgCadence = Some(Cadence.rpm(66))
  )

  val testActivityLap = RActivityLap(
    id = ActivityLapId(-1),
    activitySessionId = ActivitySessionId(-1),
    trigger = Some(LapTrigger.Distance),
    sport = Sport.Cycling,
    subSport = SubSport.Generic,
    startTime = Instant.parse("2023-03-22T21:22:34Z"),
    endTime = Instant.parse("2023-03-22T22:21:34Z"),
    movingTime = Some(Duration.ofSeconds(5640)),
    elapsedTime = Some(Duration.ofSeconds(6670)),
    distance = Distance.km(36.1),
    startPosition =
      Option(Position(Semicircle.semicircle(154655L), Semicircle.semicircle(9944546L))),
    endPosition =
      Option(Position(Semicircle.semicircle(154855L), Semicircle.semicircle(9945546L))),
    calories = Calories.kcal(246),
    totalAscend = Some(Distance.meter(351)),
    totalDescend = Some(Distance.meter(355)),
    minTemp = Some(Temperature.celcius(-2)),
    maxTemp = Some(Temperature.celcius(4)),
    avgTemp = Some(Temperature.celcius(1.6)),
    minHr = Some(HeartRate.bpm(118)),
    maxHr = Some(HeartRate.bpm(190)),
    avgHr = Some(HeartRate.bpm(161)),
    maxSpeed = Some(Speed.kmh(56.9)),
    avgSpeed = Some(Speed.kmh(21.4)),
    maxPower = None,
    avgPower = Some(Power.watts(211)),
    normPower = Some(Power.watts(198)),
    numPoolLength = Some(68),
    swimStroke = Some(SwimStroke.Freestyle),
    avgStrokeDistance = Some(Distance.meter(2.4)),
    strokeCount = Some(46),
    avgGrade = Some(Percent.percent(6.4)),
    maxCadence = Some(Cadence.rpm(121)),
    avgCadence = Some(Cadence.rpm(66))
  )
}

object TestData extends TestData
