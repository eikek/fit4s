package fit4s.codec
package internal

import scodec.*
import scodec.bits.BitVector
import scodec.bits.ByteOrdering
import scodec.bits.ByteVector

private[codec] object DataFieldCodec:

  def baseTypeValue(
      size: ByteSize,
      bo: ByteOrdering,
      baseType: FitBaseType
  ): Decoder[Vector[FitBaseValue]] =
    val bt =
      if (size.toBytes % baseType.size.toBytes == 0) baseType
      else FitBaseType.Uint8

    bt match
      case FitBaseType.string =>
        // string is special. in theory there could be multiple strings 0-terminated
        // but all files I've seen 1. never had this and 2. the profile field name
        // doesn't make sense for multiple strings. very often though, there are bytes
        // after the first 0-byte, which is just garbage and not decodeable
        // only decode the first value
        for
          dec <- FitBaseTypeCodec.codec1(bt, bo).map(Vector(_))
          _ <- Decoder.set(BitVector.empty)
        yield dec

      case _ =>
        FitBaseTypeCodec.decoder(bt, bo, size)

  def fieldsDecoder(definition: DefinitionMessage): Decoder[Vector[DataField]] =
    new Decoder[Vector[DataField]] {
      def decode(bits: BitVector): Attempt[DecodeResult[Vector[DataField]]] =
        val (res, rem) =
          makeFields(bits, definition.fields, Vector.empty)
        Attempt.successful(DecodeResult(res, rem))

      private def makeDataField(fd: FieldDef, fieldData: BitVector): DataField =
        val bo = definition.meta.byteOrder
        FitBaseType.byFieldDef(fd) match
          case Some(bt) =>
            val invalid = bt.isInvalid(bo, fieldData)
            baseTypeValue(fd.size, bo, bt).complete.decodeValue(fieldData) match
              case Attempt.Successful(bv) =>
                TypedDataField(definition.meta, fd, bt, bv, invalid)

              case Attempt.Failure(err) =>
                val reason = UntypedDataField.Reason.Decode(bt, err)
                UntypedDataField(definition.meta, fd, reason, fieldData)

          case None =>
            val reason = UntypedDataField.Reason.NoBaseType
            UntypedDataField(definition.meta, fd, reason, fieldData)

      @annotation.tailrec
      final def makeFields(
          data: BitVector,
          fields: List[FieldDef],
          current: Vector[DataField]
      ): (Vector[DataField], BitVector) =
        fields match
          case Nil        => (current, data)
          case fd :: tail =>
            val (fieldData, remain) = data.splitAt(fd.size.toBits)
            val f = makeDataField(fd, fieldData)
            makeFields(remain, tail, current.appended(f))
    }

  def fieldEncoder: Encoder[DataField] =
    Encoder {
      case UntypedDataField(_, _, _, data)                   => Attempt.successful(data)
      case TypedDataField(meta, fieldDef, baseType, data, _) =>
        FitBaseTypeCodec.encoder(meta.byteOrder, baseType).encode(data).map { result =>
          if result.size < fieldDef.size.toBits then
            appendInvalid(
              result,
              fieldDef.size.toBits,
              baseType.invalidValue(meta.byteOrder)
            )
          else result
        }
    }

  @annotation.tailrec
  private def appendInvalid(bv: BitVector, targetSize: Long, iv: ByteVector): BitVector =
    if bv.length > targetSize then bv.take(targetSize)
    else if bv.length == targetSize then bv
    else appendInvalid(bv ++ iv.bits, targetSize, iv)

  def fieldsEncoder: Encoder[Vector[DataField]] =
    Encoder(v => fieldEncoder.encodeAll(v))

  def codec(definition: DefinitionMessage): Codec[Vector[DataField]] =
    Codec(fieldsEncoder, fieldsDecoder(definition))
