package fit4s.data

import fit4s.FitMessage.DataMessage
import fit4s.profile.messages.FileIdMsg
import fit4s.profile.types.{FaveroProduct, GarminProduct}

sealed trait DeviceProduct {
  def name: String
}

object DeviceProduct {
  case object Unknown extends DeviceProduct {
    val name = "Unknown Device"
  }
  case class Garmin(product: GarminProduct) extends DeviceProduct {
    val name = product.toString
  }
  case class Favero(product: FaveroProduct) extends DeviceProduct {
    val name = product.toString
  }

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
