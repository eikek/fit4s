package fit4s.profile.types

import scodec.Codec
import scodec.bits.ByteOrdering
import scodec.codecs._

final case class LongArrayFieldType(
    rawValue: Long,
    more: List[Long],
    fitBaseType: FitBaseType
) extends GenFieldType {

  override def typeName = "long[]"

  def toList: List[Long] = rawValue :: more
}

object LongArrayFieldType {

  def codec(
      sizeBytes: Int,
      bo: ByteOrdering,
      base: FitBaseType
  ): Codec[LongArrayFieldType] = {
    val bc = BaseTypeCodec
      .baseCodec(base)(bo)
    if (sizeBytes > BaseTypeCodec.length(base)) {
      fixedSizeBytes(sizeBytes, list(bc))
        .xmap(ns => LongArrayFieldType(ns.head, ns.tail, base), v => v.rawValue :: v.more)
    } else {
      bc.xmap(n => LongArrayFieldType(n, Nil, base), _.rawValue)
    }
  }
}
