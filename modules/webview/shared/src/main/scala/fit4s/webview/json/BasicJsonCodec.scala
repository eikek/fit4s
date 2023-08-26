package fit4s.webview.json

import java.time.{Instant, ZoneId}

import cats.data.NonEmptyList
import cats.syntax.all.*
import fs2.io.file.Path

import fit4s.activities.data.*
import fit4s.common.borer.DataJsonCodec
import fit4s.common.borer.syntax.all.*
import fit4s.data.Distance
import fit4s.profile.types.Sport

import io.bullet.borer.NullOptions.*
import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

trait BasicJsonCodec extends DataJsonCodec {
  implicit def pathDecoder: Decoder[Path] =
    Decoder.forString.map(Path.apply)

  implicit def pathEncoder: Encoder[Path] =
    Encoder.forString.contramap(_.absolute.toString)

  implicit val locationDecoder: Decoder[Location] = deriveDecoder
  implicit val locationEncoder: Encoder[Location] = deriveEncoder

  implicit val deviceInfoEncoder: Encoder[DeviceInfo] =
    Encoder.forString.contramap(_.name)

  implicit val deviceInfoDecoder: Decoder[DeviceInfo] =
    Decoder.forString.map(DeviceInfo.fromString)

  implicit val activityDecoder: Decoder[Activity] = deriveDecoder
  implicit val activityEncoder: Encoder[Activity] = deriveEncoder

  implicit val sessionEncoder: Encoder[ActivitySession] =
    Encoder { (w, s) =>
      implicit val sport: Sport = s.sport
      val enc = deriveEncoder[ActivitySession]
      enc.write(w, s)
    }

  implicit val sessionDecoder: Decoder[ActivitySession] =
    deriveDecoder

  implicit val activityLapEncoder: Encoder[ActivityLap] =
    Encoder { (w, s) =>
      implicit val sport: Sport = s.sport
      val enc = deriveEncoder[ActivityLap]
      enc.write(w, s)
    }

  implicit val activityLapDecoder: Decoder[ActivityLap] =
    deriveDecoder

  implicit val sessionDataEncoder: Encoder[ActivitySessionData] =
    Encoder { (w, s) =>
      implicit val sport: Sport = Sport.Cycling
      val enc = deriveEncoder[ActivitySessionData]
      enc.write(w, s)
    }

  implicit val sessionDataDecoder: Decoder[ActivitySessionData] =
    deriveDecoder

  implicit val activityListResultEncoder: Encoder[ActivityListResult] =
    deriveEncoder[ActivityListResult]

  implicit val activityListResultDecoder: Decoder[ActivityListResult] =
    deriveDecoder

  // must provide special encoders for the maps, because JSON doesn't
  // support integers as map keys
  implicit def sessionIdToLapEncoder: Encoder[Map[ActivitySessionId, List[ActivityLap]]] =
    stringMapEncoder[List[ActivityLap]].contramap(v =>
      v.map(x => x._1.id.toString -> x._2)
    )
  implicit def sessionIdToLapDecoder: Decoder[Map[ActivitySessionId, List[ActivityLap]]] =
    stringMapDecoder[List[ActivityLap]].emap(m =>
      m.toList
        .traverse { case (keyStr, value) =>
          keyStr.toLongOption
            .toRight(s"Invalid ActivitySessionId: $keyStr")
            .map(k => ActivitySessionId(k) -> value)
        }
        .map(_.toMap)
    )

  implicit def sessionIdToGeoPlaceEncoder: Encoder[Map[ActivitySessionId, GeoPlace]] =
    stringMapEncoder[GeoPlace].contramap(v => v.map(x => x._1.id.toString -> x._2))

  implicit def sessionIdToGeoPlaceDecoder: Decoder[Map[ActivitySessionId, GeoPlace]] =
    stringMapDecoder[GeoPlace].emap(m =>
      m.toList
        .traverse { case (keyStr, value) =>
          keyStr.toLongOption
            .toRight(s"Invalid ActivitySessionId: $keyStr")
            .map(k => ActivitySessionId(k) -> value)
        }
        .map(_.toMap)
    )

  implicit def sessionIdToDistanceEncoder: Encoder[Map[ActivitySessionId, Distance]] =
    stringMapEncoder[Distance].contramap(v => v.map(x => x._1.id.toString -> x._2))
  implicit def sessionIdToDistanceDecoder: Decoder[Map[ActivitySessionId, Distance]] =
    stringMapDecoder[Distance].emap(m =>
      m.toList
        .traverse { case (keyStr, value) =>
          keyStr.toLongOption
            .toRight(s"Invalid ActivitySessionId: $keyStr")
            .map(k => ActivitySessionId(k) -> value)
        }
        .map(_.toMap)
    )

  implicit def sessionIdToDataEncoder
      : Encoder[Map[ActivitySessionId, List[ActivitySessionData]]] =
    stringMapEncoder[List[ActivitySessionData]].contramap(v =>
      v.map(x => x._1.id.toString -> x._2)
    )
  implicit def sessionIdToDataDecoder
      : Decoder[Map[ActivitySessionId, List[ActivitySessionData]]] =
    stringMapDecoder[List[ActivitySessionData]].emap(m =>
      m.toList
        .traverse { case (keyStr, value) =>
          keyStr.toLongOption
            .toRight(s"Invalid ActivitySessionId: $keyStr")
            .map(k => ActivitySessionId(k) -> value)
        }
        .map(_.toMap)
    )

  implicit val activityDetailsEncoder: Encoder[ActivityDetailResult] =
    deriveEncoder[ActivityDetailResult]

  implicit val activityDetailsDecoder: Decoder[ActivityDetailResult] =
    deriveDecoder

  implicit val activitySessionSummaryEncoder: Encoder[ActivitySessionSummary] = Encoder {
    (w, s) =>
      implicit val sport: Sport = s.sport
      val enc = deriveEncoder[ActivitySessionSummary]
      enc.write(w, s)
  }

  implicit val activitySessionSummaryDecoder: Decoder[ActivitySessionSummary] =
    deriveDecoder
}

object BasicJsonCodec extends BasicJsonCodec
