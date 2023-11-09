package fit4s.geocode

import fit4s.common.borer.CoreJsonCodec
import fit4s.common.borer.syntax.all.*
import fit4s.data.Semicircle
import fit4s.geocode.data.{Address, BoundingBox, Place}

import io.bullet.borer.*
import io.bullet.borer.derivation.MapBasedCodecs.*

trait NominatimDecoder {
  implicit val addressDecoder: Decoder[Address] =
    CoreJsonCodec.stringMapDecoder[String].map(Address.fromMap)

  implicit val placeDecoder: Decoder[Place] = deriveDecoder

  implicit val semicircleDecoder: Decoder[Semicircle] =
    Decoder.forString
      .emap(s => s.toDoubleOption.toRight(s"Invalid double for coordinate: $s"))
      .map(Semicircle.degree)

  implicit val boundingboxDecoder: Decoder[BoundingBox] =
    Decoder.forArray[String].map(_.toList).emap {
      case lat1 :: lat2 :: lng1 :: lng2 :: Nil =>
        for {
          x1 <- lat1.toDoubleOption
            .toRight(s"Invalid double: $lat1")
            .map(Semicircle.degree)
          x2 <- lat2.toDoubleOption
            .toRight(s"Invalid double: $lat2")
            .map(Semicircle.degree)
          y1 <- lng1.toDoubleOption
            .toRight(s"Invalid double: $lng1")
            .map(Semicircle.degree)
          y2 <- lng2.toDoubleOption
            .toRight(s"Invalid double: $lng2")
            .map(Semicircle.degree)
        } yield BoundingBox(x1, x2, y1, y2)
      case v => Left(s"Unexpected bounding box format: $v")
    }
}
