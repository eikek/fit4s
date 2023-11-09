package fit4s.cli

import fs2.io.file.Path

import fit4s.activities.data.*
import fit4s.common.borer.DataJsonCodec.*
import fit4s.geocode.data.{BoundingBox, NominatimOsmId, NominatimPlaceId}

import io.bullet.borer.NullOptions.*
import io.bullet.borer.*

object RecordJsonEncoder {
  implicit val pathEncoder: Encoder[Path] =
    Encoder.forString.contramap(_.absolute.toString)

  implicit val locationAndCountEncoder: Encoder[(Location, Long)] =
    Encoder { case (w, (loc, count)) =>
      w.writeMapStart()
      w.writeMapMember("count", count)
      w.write("location")
      w.writeMapStart()
      w.writeMapMember("id", loc.id)
      w.writeMapMember("path", loc.location)
      w.writeMapClose()
      w.writeMapClose()
    }

  implicit val geoPlaceEncoder: Encoder[GeoPlace] =
    Encoder { (w, p) =>
      w.writeMapStart()
      w.writeMapMember("osmPlaceId", p.osmPlaceId)
      w.writeMapMember("osmId", p.osmId)
      w.writeMapMember("position", p.position)
      w.writeMapMember("road", p.road)
      w.writeMapMember("location", p.location)
      w.writeMapMember("country", p.country)
      w.writeMapMember("countryCode", p.countryCode)
      w.writeMapMember("postCode", p.postCode)
      w.writeMapMember("boundingBox", p.boundingBox)
      w.writeMapClose()
    }

  implicit def lapEncoder: Encoder[ActivityLap] =
    Encoder { (w, l) =>
      implicit val sport = l.sport
      w.writeMapStart()
      w.writeMapMember("sport", l.sport)
      w.writeMapMember("subSport", l.subSport)
      w.writeMapMember("trigger", l.trigger)
      w.writeMapMember("startTime", l.startTime)
      w.writeMapMember("endTime", l.endTime)
      w.writeMapMember("movingTime", l.movingTime)
      w.writeMapMember("elapsedTime", l.elapsedTime)
      w.writeMapMember("distance", l.distance)
      w.writeMapMember("startPosition", l.startPosition)
      w.writeMapMember("endPosition", l.endPosition)
      w.writeMapMember("calories", l.calories)
      w.writeMapMember("totalAscend", l.totalAscend)
      w.writeMapMember("totalDescend", l.totalDescend)
      w.writeMapMember("minTemp", l.minTemp)
      w.writeMapMember("maxTemp", l.maxTemp)
      w.writeMapMember("avgTemp", l.avgTemp)
      w.writeMapMember("minHr", l.minHr)
      w.writeMapMember("maxHr", l.maxHr)
      w.writeMapMember("avgHr", l.avgHr)
      w.writeMapMember("maxSpeed", l.maxSpeed)
      w.writeMapMember("avgSpeed", l.avgSpeed)
      w.writeMapMember("maxPower", l.maxPower)
      w.writeMapMember("avgPower", l.avgPower)
      w.writeMapMember("normPower", l.normPower)
      w.writeMapMember("maxCadence", l.maxCadence)
      w.writeMapMember("avgCadence", l.avgCadence)
      w.writeMapMember("numPoolLengths", l.numPoolLength)
      w.writeMapMember("swimStroke", l.swimStroke)
      w.writeMapMember("avgStrokeDistance", l.avgStrokeDistance)
      w.writeMapMember("avgStrokeCount", l.strokeCount)
      w.writeMapMember("avgGrade", l.avgGrade)
      w.writeMapClose()
    }

  implicit def sessionEncoder: Encoder[ActivitySession] =
    Encoder { (w, s) =>
      implicit val sport = s.sport
      w.writeMapStart()
      w.writeMapMember("sport", s.sport)
      w.writeMapMember("subSport", s.subSport)
      w.writeMapMember("startTime", s.startTime)
      w.writeMapMember("endTime", s.endTime)
      w.writeMapMember("movingTime", s.movingTime)
      w.writeMapMember("elapsedTime", s.elapsedTime)
      w.writeMapMember("distance", s.distance)
      w.writeMapMember("startPosition", s.startPosition)
      w.writeMapMember("calories", s.calories)
      w.writeMapMember("totalAscend", s.totalAscend)
      w.writeMapMember("totalDescend", s.totalDescend)
      w.writeMapMember("minTemp", s.minTemp)
      w.writeMapMember("maxTemp", s.maxTemp)
      w.writeMapMember("avgTemp", s.avgTemp)
      w.writeMapMember("minHr", s.minHr)
      w.writeMapMember("maxHr", s.maxHr)
      w.writeMapMember("avgHr", s.avgHr)
      w.writeMapMember("maxSpeed", s.maxSpeed)
      w.writeMapMember("avgSpeed", s.avgSpeed)
      w.writeMapMember("maxPower", s.maxPower)
      w.writeMapMember("avgPower", s.avgPower)
      w.writeMapMember("normPower", s.normPower)
      w.writeMapMember("maxCadence", s.maxCadence)
      w.writeMapMember("avgCadence", s.avgCadence)
      w.writeMapMember("numPoolLengths", s.numPoolLength)
      w.writeMapMember("if", s.iff)
      w.writeMapMember("swimStroke", s.swimStroke)
      w.writeMapMember("avgStrokeDistance", s.avgStrokeDistance)
      w.writeMapMember("avgStrokeCount", s.avgStrokeCount)
      w.writeMapMember("poolLength", s.poolLength)
      w.writeMapMember("avgGrade", s.avgGrade)
      w.writeMapClose()
    }

  implicit def activityEncoder: Encoder[(Activity, Location)] =
    Encoder { case (w, (a, loc)) =>
      w.writeMapStart()
      w.writeMapMember("id", a.id)
      w.writeMapMember("location", loc.location)
      w.writeMapMember("file", loc.locationPath / a.path)
      w.writeMapMember("fileId", a.activityFileId)
      w.writeMapMember("fileIdString", a.activityFileId.asString)
      w.writeMapMember("created", a.created)
      w.writeMapMember("name", a.name)
      w.writeMapMember("timestamp", a.timestamp)
      w.writeMapMember("totalTime", a.totalTime)
      w.writeMapMember("notes", a.notes)
      w.writeMapMember("importedAt", a.importDate)
      w.writeMapClose()
    }

  implicit def encodeList: Encoder[ActivityListResult] =
    Encoder { (w, r) =>
      w.writeMapStart()
      w.writeMapMember("activity", r.activity -> r.location)
      w.writeMapMember("sessions", r.sessions)
      w.writeMapMember("tags", r.tags)
      w.writeMapClose()
    }

  implicit def encodeDetail: Encoder[ActivityDetailResult] =
    Encoder { (w, r) =>
      w.writeMapStart()
      w.writeMapMember("activity", r.activity -> r.location)
      w.writeMapMember("sessions", r.sessions)
      w.writeMapMember("tags", r.tags)
      w.writeMapMember("stravaId", r.stravaId)
      w.writeMapMember("laps", r.laps)
      w.writeMapMember("startPlace", r.startPlace)
      w.writeMapMember("endPlace", r.endPlace)
      w.writeMapMember("startEndDistance", r.startEndDistance)
      w.writeMapClose()
    }
}
