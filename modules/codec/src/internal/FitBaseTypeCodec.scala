package fit4s.codec
package internal

import scodec.*
import scodec.bits.ByteOrdering.{BigEndian, LittleEndian}
import scodec.bits.{BitVector, ByteOrdering}

private[codec] object FitBaseTypeCodec:

  def codec(
      baseType: FitBaseType,
      bo: ByteOrdering,
      fieldSize: ByteSize
  ): Codec[Vector[FitBaseValue]] =
    Codec(encoder(bo, baseType), decoder(baseType, bo, fieldSize))

  def decoder(
      baseType: FitBaseType,
      bo: ByteOrdering,
      fieldSize: ByteSize
  ): Decoder[Vector[FitBaseValue]] =
    val bt =
      if (fieldSize.toBytes % baseType.size.toBytes == 0) baseType
      else FitBaseType.Uint8

    bt match
      case FitBaseType.string =>
        // string is special. in theory there could be multiple strings 0-terminated
        // but all files I've seen 1. never had this and 2. the profile field name
        // doesn't make sense for multiple strings. very often though, there are bytes
        // after the first 0-byte, which is just garbage and not decodeable
        // only decode the first value
        for
          dec <- codec1(bt, bo).map(Vector(_))
          _ <- Decoder.set(BitVector.empty)
        yield dec

      case _ =>
        Codecs.collect[Vector, FitBaseValue](codec1(bt, bo))

  def encoder(bo: ByteOrdering, ft: FitBaseType): Encoder[Vector[FitBaseValue]] =
    Encoder(encoder1(bo, ft).encodeAll)

  def codec1(bt: FitBaseType, bo: ByteOrdering): Codec[FitBaseValue] =
    bt match
      case v: FitBaseType.IntBased =>
        Codec(encoder1(bo, v), intCodec(bo, v.size, v.signed).asDecoder)
      case v: FitBaseType.LongBased =>
        Codec(encoder1(bo, v), longCodec(bo, v.size, v.signed).asDecoder)
      case v: FitBaseType.FloatBased =>
        Codec(encoder1(bo, v), floatCodec(bo, v.size).asDecoder)
      case v: FitBaseType.FByte.type =>
        Codec(encoder1(bo, v), scodec.codecs.byte.asDecoder)
      case v: FitBaseType.string.type =>
        Codec(encoder1(bo, v), Codecs.stringUtf8)

  private def signed(ft: FitBaseType): Boolean = ft match
    case v: FitBaseType.IntBased  => v.signed
    case v: FitBaseType.LongBased => v.signed
    case _                        => false

  private def encoder1(bo: ByteOrdering, ft: FitBaseType): Encoder[FitBaseValue] =
    Encoder {
      case n: Int    => intCodec(bo, ft.size, signed(ft)).encode(n)
      case n: Long   => longCodec(bo, ft.size, signed(ft)).encode(n)
      case b: Byte   => scodec.codecs.byte.encode(b)
      case d: Double => floatCodec(bo, ft.size).encode(d)
      case s: String => Codecs.stringUtf8.encode(s)
    }

  private def intCodec(bo: ByteOrdering, size: ByteSize, signed: Boolean): Codec[Int] =
    (bo, signed) match
      case (BigEndian, true) =>
        scodec.codecs.int(size.toBits.toInt)
      case (BigEndian, false) =>
        scodec.codecs.uint(size.toBits.toInt)
      case (LittleEndian, true) =>
        scodec.codecs.intL(size.toBits.toInt)
      case (LittleEndian, false) =>
        scodec.codecs.uintL(size.toBits.toInt)

  private def longCodec(bo: ByteOrdering, size: ByteSize, signed: Boolean) =
    (bo, signed) match
      case (BigEndian, true) =>
        scodec.codecs.long(size.toBits.toInt)
      case (BigEndian, false) =>
        scodec.codecs.ulong(size.toBits.toInt)
      case (LittleEndian, true) =>
        scodec.codecs.longL(size.toBits.toInt)
      case (LittleEndian, false) =>
        scodec.codecs.ulongL(size.toBits.toInt)

  private def floatCodec(bo: ByteOrdering, size: ByteSize): Codec[Double] =
    (bo, size.toBits) match
      case (BigEndian, 32) =>
        scodec.codecs.float.xmap(_.toDouble, _.toFloat)
      case (BigEndian, 64) =>
        scodec.codecs.double
      case (LittleEndian, 32) =>
        scodec.codecs.floatL.xmap(_.toDouble, _.toFloat)
      case (LittleEndian, 64) =>
        scodec.codecs.doubleL
      case _ =>
        sys.error(s"Programming error: no codec for arch/size: $bo/$size")
