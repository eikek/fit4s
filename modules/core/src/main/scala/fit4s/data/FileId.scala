package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.FileIdMsg
import fit4s.profile.types.{DateTime, File, Manufacturer}

import scodec.Codec
import scodec.bits.{Bases, ByteOrdering, ByteVector}
import scodec.codecs._

final case class FileId(
    fileType: File,
    manufacturer: Manufacturer,
    product: DeviceProduct,
    serialNumber: Option[Long],
    createdAt: Option[DateTime],
    number: Option[Long],
    productName: Option[String]
):

  def asString: String =
    FileId.idCodec
      .encode(this)
      .map(_.toBase58(Bases.Alphabets.Base58))
      .toEither
      .fold(err => sys.error(err.messageWithContext), identity)

object FileId:

  private def idCodec: Codec[FileId] =
    (File.codec(ByteOrdering.LittleEndian) ::
      Manufacturer.codec(ByteOrdering.LittleEndian) ::
      DeviceProduct.codec ::
      optional(bool, uint32L) ::
      optional(
        bool,
        DateTime
          .codec(ByteOrdering.LittleEndian)
      )
      ::
      optional(bool, uint32) ::
      optional(bool, cstring)).as[FileId]

  def fromString(str: String): Either[String, FileId] =
    ByteVector
      .fromBase58Descriptive(str, Bases.Alphabets.Base58)
      .flatMap(bv =>
        idCodec.complete.decode(bv.bits).toEither.left.map(_.messageWithContext)
      )
      .map(_.value)

  def unsafeFromString(str: String): FileId =
    fromString(str).fold(sys.error, identity)

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
