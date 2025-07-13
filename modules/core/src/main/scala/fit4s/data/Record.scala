package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.RecordMsg
import fit4s.profile.types.DateTime

final case class Record(
    timestamp: DateTime,
    position: Option[Position],
    altitude: Option[Distance],
    heartRate: Option[HeartRate],
    cadence: Option[Cadence],
    distance: Option[Distance],
    speed: Option[Speed],
    power: Option[Power],
    grade: Option[Grade],
    temperature: Option[Temperature],
    calories: Option[Calories]
)

object Record:

  def from(recordMsg: DataMessage): Either[String, Record] =
    if (!recordMsg.isMessage(RecordMsg)) Left(s"Not a record: $recordMsg")
    else
      for {
        time <- recordMsg.getRequiredField(RecordMsg.timestamp)
        lat <- recordMsg.getField(RecordMsg.positionLat)
        lng <- recordMsg.getField(RecordMsg.positionLong)
        alt1 <- recordMsg.getField(RecordMsg.enhancedAltitude)
        alt2 <- recordMsg.getField(RecordMsg.altitude)
        alt = alt1.orElse(alt2)
        hr <- recordMsg.getField(RecordMsg.heartRate)
        cad <- recordMsg.getField(RecordMsg.cadence)
        dst <- recordMsg.getField(RecordMsg.distance)
        spd1 <- recordMsg.getField(RecordMsg.enhancedSpeed)
        spd2 <- recordMsg.getField(RecordMsg.speed)
        spd = spd1.orElse(spd2)
        pwr <- recordMsg.getField(RecordMsg.power)
        grd <- recordMsg.getField(RecordMsg.grade)
        temp <- recordMsg.getField(RecordMsg.temperature)
        cal <- recordMsg.getField(RecordMsg.calories)
      } yield Record(
        time.value,
        Position.optional(lat.flatMap(_.semicircle), lng.flatMap(_.semicircle)),
        alt.flatMap(_.distance),
        hr.flatMap(_.heartrate),
        cad.flatMap(_.cadence),
        dst.flatMap(_.distance),
        spd.flatMap(_.speed),
        pwr.flatMap(_.power),
        grd.flatMap(_.grade),
        temp.flatMap(_.temperature),
        cal.flatMap(_.calories)
      )
