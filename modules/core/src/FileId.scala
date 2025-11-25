package fit4s.core

import java.time.Instant

import fit4s.core.MessageReader as MR
import fit4s.core.data.DateTime
import fit4s.profile.*

import scodec.Attempt
import scodec.Codec
import scodec.Err
import scodec.bits.Bases.Alphabets
import scodec.bits.BitVector

final case class FileId(
    fileType: ProfileEnum,
    manufacturer: ProfileEnum,
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

  def asStringLegacy: String =
    FileId.legacyIdCodec
      .encode(this)
      .map(FileId.bitsToString)
      .toEither
      .fold(err => sys.error(err.messageWithContext), identity)

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
        MR.field(m.manufacturer).asEnum ::
        MR.field(m.product).asEnum.option ::
        MR.field(m.serialNumber).as[Long].option ::
        MR.field(m.timeCreated).as[Instant].option ::
        MR.field(m.number).as[Int].option ::
        MR.field(m.productName).as[String].option.tuple).as[FileId]
    }

  def fromString(str: String): Either[String, FileId] =
    decode(codec, str)

  def fromStringLegacy(str: String): Either[String, FileId] =
    decode(legacyIdCodec, str)

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

  private val legacyIdCodec: Codec[FileId] = {
    // this does the same as the variant from previous versions to retain compatibility with old values.
    import scodec.codecs.*
    def c(inner: Codec[Int], p: ProfileType, pn: ProfileType*): Codec[ProfileEnum] =
      inner.xmap(n => ProfileEnum.unsafe(n.toInt, p, pn*), _.ordinal)

    val fileType = c(uint8, FileType)
    val manu = c(uint16L, ManufacturerType)
    val product = uint4L.consume {
      case 0 => provide(Option.empty[ProfileEnum])
      case 1 => c(uint16L, GarminProductType).xmap(Some(_), _.get)
      case 2 => c(uint16L, FaveroProductType).xmap(Some(_), _.get)
      case v => fail(Err(s"Unknown product type: $v"))
    } {
      case None     => 0
      case Some(pt) =>
        if (pt.profile == GarminProductType) ManufacturerType.garmin
        else ManufacturerType.faveroElectronics
    }
    val serial = optional(bool, ulongL(32))
    val created = optional(
      bool,
      ulongL(32).xmap(s => DateTime(s).asInstant, i => DateTime.fromInstant(i).value)
    )
    val number = optional(bool, ulongL(32).xmap(_.toInt, _.toLong))
    val name = optional(bool, cstring)
    (fileType :: manu :: product :: serial :: created :: number :: name).as[FileId]
  }

  private val codec: Codec[FileId] = {
    import scodec.codecs.*

    def c(inner: Codec[Int], p: ProfileType, pt: ProfileType*): Codec[ProfileEnum] =
      inner.exmap(
        n =>
          Attempt.fromOption(
            ProfileEnum.first(n, p, pt*),
            Err(s"Invalid profile enum '$n' for type ${p.name}")
          ),
        en => Attempt.successful(en.ordinal)
      )

    val fileType = c(uint8, FileType)
    val serial = optional(bool, ulongL(32))
    val created = optional(
      bool,
      ulongL(32).xmap(s => DateTime(s).asInstant, i => DateTime.fromInstant(i).value)
    )
    val number = optional(bool, ulongL(32).xmap(_.toInt, _.toLong))
    val name = optional(bool, cstring)
    val manu = c(uint16L, ManufacturerType)

    val fields = manu.flatPrepend { m =>
      (if m.isValue(ManufacturerType.garmin)
       then optional(bool, c(uint16L, GarminProductType))
       else if m.isValue(ManufacturerType.faveroElectronics)
       then optional(bool, c(uint16L, FaveroProductType))
       // fallback to something if it is provided
       else optional(bool, c(uint16L, GarminProductType, FaveroProductType)))
      :: serial :: created :: number :: name
    }
    (fileType :: fields).as[FileId]
  }
