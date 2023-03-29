package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.FileIdMsg
import fit4s.profile.types.{FaveroProduct, GarminProduct}
import scodec.{Codec, Err}
import scodec.bits.ByteOrdering

sealed trait DeviceProduct extends Product {
  def name: String
  def widen: DeviceProduct = this
}

object DeviceProduct {
  private[fit4s] val codec: Codec[DeviceProduct] =
    scodec.codecs.uint4L.consume[DeviceProduct] {
      case 0 => scodec.codecs.provide(Unknown.widen)
      case 1 =>
        GarminProduct.codec(ByteOrdering.LittleEndian).xmapc(Garmin)(_.product).upcast
      case 2 =>
        FaveroProduct.codec(ByteOrdering.LittleEndian).xmapc(Favero)(_.product).upcast
      case _ => scodec.codecs.fail(Err(s"Unknown device product type"))
    } {
      case Unknown   => 0
      case Garmin(_) => 1
      case Favero(_) => 2
    }

  case object Unknown extends DeviceProduct {
    val name = "Unknown Device"
  }
  case class Garmin(product: GarminProduct) extends DeviceProduct {
    val name = product.toString
  }
  case class Favero(product: FaveroProduct) extends DeviceProduct {
    val name = product.toString
  }

  val all: List[DeviceProduct] =
    Unknown :: GarminProduct.all.map(Garmin) ::: FaveroProduct.all.map(Favero)

  def from(fileIdMsg: DataMessage): Either[String, DeviceProduct] =
    fileIdMsg.definition.profileMsg match {
      case Some(FileIdMsg) =>
        for {
          gp <- fileIdMsg.getField(FileIdMsg.productGarminProduct)
          fp <- fileIdMsg.getField(FileIdMsg.productFaveroProduct)
          r = gp
            .map(_.value)
            .map(Garmin)
            .orElse(fp.map(_.value).map(Favero))
            .getOrElse(Unknown)
        } yield r

      case _ =>
        Left(
          s"Message '${fileIdMsg.definition.profileMsg}' doesn't contain a product field."
        )
    }
}
