package fit4s.activities.records

import fit4s.activities.data.{ActivityId, ActivitySessionId, LocationId}
import fit4s.data._
import fit4s.profile.types.{DateTime, File, GarminProduct, Manufacturer, Sport, SubSport}

import java.time.{Duration, Instant}

trait TestData {

  val testActivity = ActivityRecord(
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
    notes = Some("this is a note")
  )

  val testActivitySession = ActivitySessionRecord(
    id = ActivitySessionId(-1),
    activityId = ActivityId(-1),
    sport = Sport.Cycling,
    subSport = SubSport.Generic,
    startTime = Instant.parse("2023-03-22T21:22:34Z"),
    endTime = Instant.parse("2023-03-22T22:21:34Z"),
    movingTime = Duration.ofSeconds(5640),
    elapsedTime = Duration.ofSeconds(6670),
    distance = Distance.km(36.1),
    startPositionLat = Option(Semicircle.semicircle(154655L)),
    startPositionLong = Option(Semicircle.semicircle(9944546L)),
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
    avgPower = Some(Power.watts(211))
  )
}

object TestData extends TestData
