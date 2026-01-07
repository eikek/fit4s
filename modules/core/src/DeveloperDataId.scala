package fit4s.core

import java.util.UUID

import fit4s.core.MessageReader as MR
import fit4s.profile.DeveloperDataIdMsg
import fit4s.profile.ProfileEnum

import scodec.bits.ByteVector

final case class DeveloperDataId(
    devIndex: Int,
    applicationId: ByteVector,
    appVersion: Long,
    developerId: Option[ByteVector],
    manufacturer: Option[ProfileEnum]
):
  def applicationUUID: Option[UUID] =
    Option.when(applicationId.size == 16)(applicationId.toUUID)

object DeveloperDataId:

  given MR[DeveloperDataId] =
    MR.forMsg(DeveloperDataIdMsg) { m =>
      (MR.field(m.developerDataIndex).as[Int] ::
        MR.field(m.applicationId).as[ByteVector] ::
        MR.field(m.applicationVersion).as[Long] ::
        MR.field(m.developerId).as[ByteVector].option ::
        MR.field(m.manufacturerId).asEnum.option.tuple).as[DeveloperDataId]
    }
