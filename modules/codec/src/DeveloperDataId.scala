package fit4s.codec

import java.util.UUID

import fit4s.codec.FieldDescription.Field
import fit4s.codec.FitBaseValue$package.FitBaseValue.syntax.*

import scodec.bits.ByteVector

/** Represents a developer-data-id msg in a fit file that identifies developer fields. It
  * associates the field-description messages to a specific application.
  */
final case class DeveloperDataId(
    devIndex: Int,
    applicationId: ByteVector,
    appVersion: Long,
    developerId: Option[ByteVector],
    manufacturer: Option[Int]
):
  def applicationUUID: Option[UUID] =
    Option.when(applicationId.size == 16)(applicationId.toUUID)

object DeveloperDataId:
  val globalMesgNum = 207

  def read(r: DataRecord): Option[DeveloperDataId] =
    if r.globalMessage != globalMesgNum then None
    else
      for
        devIdx <- Profile.devDataIdx.firstValue(r).flatMap(_.asInt)
        appId <- r
          .fieldData(Profile.applicationId.fieldDefNum)
          .map(_.flatMap(_.asByte))
          .map(ByteVector.apply)
        appVer <- Profile.appVersion.firstValue(r).flatMap(_.asLong)
        devId = r
          .fieldData(Profile.developerId.fieldDefNum)
          .map(_.flatMap(_.asByte))
          .map(ByteVector.apply)
        manuId = Profile.manufacturerId
          .firstValue(r)
          .flatMap(_.asInt)
      yield DeveloperDataId(devIdx, appId, appVer, devId, manuId)

  object Profile:
    val developerId = Field(0, "developer_id", FitBaseType.FByte)
    val applicationId = Field(1, "application_id", FitBaseType.FByte)
    val manufacturerId = Field(2, "manufacturer_id", FitBaseType.Uint16)
    val devDataIdx = Field(3, "developer_data_index", FitBaseType.Uint8)
    val appVersion = Field(4, "application_version", FitBaseType.Uint32)
