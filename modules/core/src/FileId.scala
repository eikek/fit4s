package fit4s.core

import java.time.Instant

import fit4s.codec.FitBaseType
import fit4s.core.MessageReader as MR
import fit4s.core.data.DateTime
import fit4s.profile.*

import scodec.Codec
import scodec.bits.Bases.Alphabets
import scodec.bits.BitVector

final case class FileId(
    fileType: ProfileEnum,
    manufacturer: Option[ProfileEnum],
    product: Option[ProfileEnum],
    serialNumber: Option[Long],
    createdAt: Option[Instant],
    number: Option[Int],
    productName: Option[String]
):
  def isActivity: Boolean =
    fileType.ordinal == FileType.activity

  def isCourse: Boolean =
    fileType.ordinal == FileType.course

  def withSerialNumber(sn: Long): FileId =
    copy(serialNumber = Some(sn))

  def withCreated(i: Instant): FileId =
    copy(createdAt = Some(i))

  def withProductName(name: String): FileId =
    copy(productName = Some(name).filter(_.nonEmpty))

  def asString: String =
    FileId.codec
      .encode(this)
      .map(FileId.bitsToString)
      .toEither
      .fold(err => sys.error(err.messageWithContext), identity)

object FileId:

  given MR[FileId] =
    MR.forMsg(FileIdMsg) { m =>
      (MR.field(m.`type`).asEnum ::
        MR.field(m.manufacturer).asEnum.option ::
        MR.field(m.product).asEnum.option ::
        MR.field(m.serialNumber).as[Long].option ::
        MR.field(m.timeCreated).as[Instant].option ::
        MR.field(m.number).as[Int].option ::
        MR.field(m.productName).as[String].option.tuple).as[FileId]
    }

  given MessageEncoder[FileId] =
    MessageEncoder.forMsg(FileIdMsg).fields { (msg, fid) =>
      import MessageEncoder.syntax.*
      fid.fileType -> msg.`type`
        :: fid.manufacturer -> msg.manufacturer
        :: fid.product -> msg.product
        :: fid.serialNumber -> msg.serialNumber
        :: fid.createdAt -> msg.timeCreated
        :: fid.number -> msg.number
        :: fid.productName -> msg.productName
        :: Nil
    }

  def fromString(str: String): Either[String, FileId] =
    decode(codec, str)

  private def stringToBits(s: String): Either[String, BitVector] =
    BitVector.fromBase58Descriptive(s, Alphabets.Base58)

  private def bitsToString(bits: BitVector): String =
    bits.toBase58(Alphabets.Base58)

  private def decode(c: Codec[FileId], str: String): Either[String, FileId] =
    for
      bits <- stringToBits(str)
      result <- c.decode(bits).toEither.left.map(_.messageWithContext)
      // bit to string conversion right-pads with zeros if last byte is not full
      _ <-
        if result.remainder.sizeGreaterThan(7) then
          Left(s"Invalid fit file id ($str): Too many bits left for decoding")
        else Right(())
    yield result.value

  private val codec: Codec[FileId] = {
    import scodec.codecs.*

    def c(inner: Codec[Int], p: ProfileType): Codec[ProfileEnum] =
      inner.xmap(n => ProfileEnum(p, n), en => en.ordinal)

    def isValue(ptv: ProfileEnum | Int, value: Int): Boolean = ptv match
      case n: Int          => value == n
      case pt: ProfileEnum => pt.isValue(value)

    val fileType = c(uint8, FileType)
    val serial = optional(bool, ulongL(32))
    val created = optional(
      bool,
      ulongL(32).xmap(s => DateTime(s).asInstant, i => DateTime.fromInstant(i).value)
    )
    val number = optional(bool, ulongL(32).xmap(_.toInt, _.toLong))
    val name = optional(bool, cstring)
    val manu = optional(bool, c(uint16L, ManufacturerType))

    val fields = manu.flatPrepend { m =>
      val product =
        if m.exists(isValue(_, ManufacturerType.garmin))
        then optional(bool, c(uint16L, GarminProductType))
        else if m.exists(isValue(_, ManufacturerType.faveroElectronics))
        then optional(bool, c(uint16L, FaveroProductType))
        else
          optional(
            bool,
            c(uint16L, ProfileType.unknown("unknown", FitBaseType.Uint16.name))
          )

      product :: serial :: created :: number :: name
    }
    (fileType :: fields).as[FileId]
  }
