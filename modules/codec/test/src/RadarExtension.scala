package fit4s.codec.internal

import java.util.UUID

import cats.syntax.all.*

import fit4s.codec.*
import fit4s.codec.FitBaseValue.syntax.*

final class RadarExtension(fit: FitFile):
  val developerDataId =
    fit
      .findMessages(DeveloperDataId.globalMesgNum)
      .flatMap(DeveloperDataId.read)
      .find(_.applicationUUID.contains(RadarExtension.applicationId))

  val fieldDescriptions = developerDataId match
    case Some(devId) =>
      fit
        .findMessages(FieldDescription.globalMesgNum)
        .flatMap(FieldDescription.read)
        .filter(fd => fd.devDataIdx.toInt == devId.devIndex)
        .flatMap(fd => fd.fieldName.map(_ -> fd))
        .toMap
    case None => Map.empty

  def getValue(msg: DataRecord, field: RadarExtension.Field): Option[Int] =
    fieldDescriptions
      .get(field.name)
      .map(DevFieldId.apply)
      .flatMap(msg.devFieldData)
      .flatMap(_.headOption.map(_.asInt))
      .flatten

object RadarExtension:
  val applicationId = UUID.fromString("c5d949c3-9acb-4e00-bb2d-c3b871e9e733")

  enum Field(val name: String):
    case RadarLap extends Field("radar_lap")
    case RadarTotal extends Field("radar_total")
    case RadarCurrent extends Field("radar_current")

  def enabled(fit: FitFile): Boolean =
    RadarExtension(fit).developerDataId.isDefined
