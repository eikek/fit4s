package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.{DeviceInfoMsg, FileIdMsg, Msg}
import fit4s.profile.types.{FaveroProduct, GarminProduct}

import scodec.bits.ByteOrdering
import scodec.{Codec, Err}

sealed trait DeviceProduct extends Product:
  def name: String
  def widen: DeviceProduct = this

object DeviceProduct:
  private[fit4s] val codec: Codec[DeviceProduct] =
    scodec.codecs.uint4L.consume[DeviceProduct] {
      case 0 => scodec.codecs.provide(Unknown.widen)
      case 1 =>
        GarminProduct
          .codec(ByteOrdering.LittleEndian)
          .xmapc(Garmin.apply)(_.product)
          .upcast
      case 2 =>
        FaveroProduct
          .codec(ByteOrdering.LittleEndian)
          .xmapc(Favero.apply)(_.product)
          .upcast
      case _ => scodec.codecs.fail(Err(s"Unknown device product type"))
    } {
      case Unknown   => 0
      case Garmin(_) => 1
      case Favero(_) => 2
    }

  case object Unknown extends DeviceProduct:
    val name = "Unknown"
  case class Garmin(product: GarminProduct) extends DeviceProduct:
    val name = product.toString
  case class Favero(product: FaveroProduct) extends DeviceProduct:
    val name = product.toString

  val all: List[DeviceProduct] =
    Unknown :: GarminProduct.all.map(Garmin.apply) ::: FaveroProduct.all.map(Favero.apply)

  private def fromMessage(
      msg: DataMessage,
      sub1: Msg.SubField[GarminProduct],
      sub2: Msg.SubField[FaveroProduct]
  ): Either[String, DeviceProduct] =
    for {
      gp <- msg.getField(sub1)
      fp <- msg.getField(sub2)
      r = gp
        .map(_.value)
        .map(Garmin.apply)
        .orElse(fp.map(_.value).map(Favero.apply))
        .getOrElse(Unknown)
    } yield r

  def fromString(str: String): Either[String, DeviceProduct] =
    all.find(_.name.equalsIgnoreCase(str)).toRight(s"Invalid device product name: $str")

  def unsafeFromString(str: String): DeviceProduct =
    fromString(str).fold(sys.error, identity)

  def from(msg: DataMessage): Either[String, DeviceProduct] =
    msg.definition.profileMsg match
      case Some(FileIdMsg) =>
        fromMessage(msg, FileIdMsg.productGarminProduct, FileIdMsg.productFaveroProduct)

      case Some(DeviceInfoMsg) =>
        fromMessage(
          msg,
          DeviceInfoMsg.productGarminProduct,
          DeviceInfoMsg.productFaveroProduct
        )

      case _ =>
        Left(
          s"Message '${msg.definition.profileMsg}' doesn't contain a product field."
        )
