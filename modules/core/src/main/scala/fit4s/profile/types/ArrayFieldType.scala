package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs.{fixedSizeBytes, list}

final case class ArrayFieldType[A <: GenFieldType](values: List[A]) extends GenFieldType {
  val rawValue = -1L // todo remove?
  val typeName = "a[]"
}

object ArrayFieldType {

  def codecLong(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  ): Codec[ArrayFieldType[LongFieldType]] = {
    val bc = BaseTypeCodec.baseCodec(base)(bo)
    if (sizeBytes > BaseTypeCodec.length(base)) {
      fixedSizeBytes(sizeBytes, list(bc))
        .xmapc(_.map(LongFieldType(_, base)))(_.map(_.rawValue))
        .xmapc(ArrayFieldType.apply)(_.values)
    } else {
      bc.xmap(n => ArrayFieldType(List(LongFieldType(n, base))), _.values.head.rawValue)
    }
  }

  object LongArray {
    def unapply(f: ArrayFieldType[_]): Option[List[Long]] =
      f.values.headOption match {
        case Some(_: LongFieldType) =>
          Some(f.values.map(_.asInstanceOf[LongFieldType]).map(_.rawValue))
        case _ => None
      }
  }
}
