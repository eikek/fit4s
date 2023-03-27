package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.FileIdMsg
import fit4s.profile.types.{DateTime, File, Manufacturer}

final case class FileId(
    fileType: File,
    manufacturer: Manufacturer,
    product: DeviceProduct,
    serialNumber: Option[Long],
    createdAt: Option[DateTime],
    number: Option[Long],
    productName: Option[String]
)

object FileId {

  def from(fileIdMsg: DataMessage): Either[String, FileId] =
    for {
      ft <- fileIdMsg.getRequiredField(FileIdMsg.`type`)
      manuf <- fileIdMsg.getRequiredField(FileIdMsg.manufacturer)
      device <- DeviceProduct.from(fileIdMsg)
      serial <- fileIdMsg.getField(FileIdMsg.serialNumber)
      created <- fileIdMsg.getField(FileIdMsg.timeCreated)
      number <- fileIdMsg.getField(FileIdMsg.number)
      prodname <- fileIdMsg.getField(FileIdMsg.productName)
    } yield FileId(
      ft.value,
      manuf.value,
      device,
      serial.map(_.value.rawValue),
      created.map(_.value),
      number.map(_.value.rawValue),
      prodname.map(_.value.rawValue)
    )
}
