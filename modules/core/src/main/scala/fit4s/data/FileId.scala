package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.FileIdMsg
import fit4s.profile.types.{DateTime, File, Manufacturer}

final case class FileId(
    fileType: File,
    manufacturer: Manufacturer,
    product: DeviceProduct,
    serialNumber: Long,
    createdAt: Option[DateTime],
    number: Option[Long],
    productName: Option[String]
)

object FileId {

  def from(fileIdMsg: DataMessage): Either[String, FileId] =
    for {
      ft <- fileIdMsg.requireField(FileIdMsg.`type`)
      manuf <- fileIdMsg.requireField(FileIdMsg.manufacturer)
      device <- DeviceProduct.from(fileIdMsg)
      serial <- fileIdMsg.requireField(FileIdMsg.serialNumber)
      created <- fileIdMsg.findField(FileIdMsg.timeCreated)
      number <- fileIdMsg.findField(FileIdMsg.number)
      prodname <- fileIdMsg.findField(FileIdMsg.productName)
    } yield FileId(
      ft.value,
      manuf.value,
      device,
      serial.value.rawValue,
      created.map(_.value),
      number.map(_.value.rawValue),
      prodname.map(_.value.rawValue)
    )
}
