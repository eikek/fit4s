package fit4s.activities.impl

import java.time.{Instant, ZoneId}

import scala.collection.immutable.Seq

import cats.data.{NonEmptyList, OptionT}
import cats.effect.Sync
import cats.syntax.all.*

import fit4s.activities.ImportResult
import fit4s.activities.data.*
import fit4s.activities.records.*
import fit4s.data.*
import fit4s.profile.types.SubSport
import fit4s.tcx.TcxActivity

import doobie.*

object TcxActivityImport {
  def addActivity(
      tags: Set[TagId],
      locationId: LocationId,
      path: String,
      notes: Option[String],
      zone: ZoneId,
      now: Instant
  )(
      tcx: TcxActivity
  ): ConnectionIO[ImportResult[ActivityId]] = {
    val name = ActivityName.generate(tcx.id, Set(tcx.sport), zone)

    for {
      actId <- add(locationId, path, name, notes, now)(tcx)
      _ <- NonEmptyList.fromList(tags.toList) match {
        case Some(nel) =>
          actId match {
            case ImportResult.Success(id) => RActivityTag.insert1(id, nel)
            case _                        => Sync[ConnectionIO].pure(0)
          }
        case None => Sync[ConnectionIO].pure(0)
      }
    } yield actId
  }

  def add(
      locationId: LocationId,
      path: String,
      name: String,
      notes: Option[String],
      now: Instant
  )(
      tcx: TcxActivity
  ): ConnectionIO[ImportResult[ActivityId]] = {
    val insert: ConnectionIO[ImportResult[ActivityId]] =
      for {
        actId <- RActivity.insert(
          Activity( // TODO insert raw device string
            ActivityId(-1),
            locationId,
            path,
            tcx.fileId,
            tcx.deviceProduct
              .map(DeviceInfo.Product.apply)
              .orElse(tcx.creator.flatMap(_.nameNormalized).map(DeviceInfo.Name.apply))
              .getOrElse(DeviceInfo.Product(DeviceProduct.Unknown)),
            tcx.fileId.serialNumber,
            tcx.id.some,
            name,
            tcx.id,
            tcx.totalTime,
            notes,
            now
          )
        )

        _ <- addSession(actId, tcx)
      } yield ImportResult.success(actId)

    OptionT(RActivity.findByFileId(tcx.fileId))
      .map(r => ImportResult.duplicate(r.id, tcx.fileId, path))
      .getOrElseF(insert)
  }

  private def addSession(activityId: ActivityId, act: TcxActivity): ConnectionIO[
    (ActivitySessionId, Seq[ActivityLapId], Seq[ActivitySessionDataId])
  ] =
    for {
      sessionId <- RActivitySession.insert(
        ActivitySession(
          id = ActivitySessionId(-1),
          activityId = activityId,
          sport = act.sport,
          subSport = SubSport.Generic,
          startTime = act.id,
          endTime = act.endTime,
          movingTime = act.totalTime,
          elapsedTime = act.totalTime,
          distance = act.distance,
          startPosition = act.startPosition,
          calories = act.calories,
          totalAscend = act.totalAscend.some,
          totalDescend = act.totalDescend.some,
          minTemp = None,
          maxTemp = None,
          avgTemp = None,
          minHr = None,
          maxHr = act.maxHr,
          avgHr = act.avgHr,
          maxSpeed = act.maxSpeed,
          avgSpeed = act.avgSpeed,
          maxPower = None,
          avgPower = None,
          normPower = None,
          maxCadence = act.maxCadence,
          avgCadence = act.avgCadence,
          tss = None,
          numPoolLength = None,
          iff = None,
          swimStroke = None,
          avgStrokeDistance = None,
          avgStrokeCount = None,
          poolLength = None,
          avgGrade = None
        )
      )
      lapIds <- act.laps.traverse { l =>
        RActivityLap.insert(
          ActivityLap(
            id = ActivityLapId(-1),
            activitySessionId = sessionId,
            sport = act.sport,
            subSport = SubSport.Generic,
            trigger = None,
            startTime = l.startTime,
            endTime = l.endTime,
            startPosition = l.startPosition,
            endPosition = l.endPosition,
            movingTime = l.totalTime.some,
            elapsedTime = l.totalTime.some,
            calories = l.calories,
            distance = l.distance,
            minTemp = None,
            maxTemp = None,
            avgTemp = None,
            maxSpeed = l.maximumSpeed,
            avgSpeed = l.avgSpeed,
            minHr = None,
            maxHr = l.maximumHeartRate,
            avgHr = l.avgHr,
            maxPower = None,
            avgPower = None,
            normPower = None,
            maxCadence = None,
            avgCadence = l.cadence,
            totalAscend = l.totalAscend.some,
            totalDescend = l.totalDescend.some,
            numPoolLength = None,
            swimStroke = None,
            avgStrokeDistance = None,
            strokeCount = None,
            avgGrade = None
          )
        )
      }
      rIds <- act.laps.flatMap(_.track).traverse { r =>
        RActivitySessionData.insert(
          ActivitySessionData(
            id = ActivitySessionDataId(-1),
            activitySessionId = sessionId,
            timestamp = r.time,
            position = r.position,
            altitude = r.altitude,
            heartRate = r.heartRate,
            cadence = r.cadence,
            distance = r.distance,
            speed = None,
            power = None,
            grade = None,
            temperature = None,
            calories = None
          )
        )
      }
    } yield (sessionId, lapIds, rIds)
}
