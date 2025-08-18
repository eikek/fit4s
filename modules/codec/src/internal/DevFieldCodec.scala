package fit4s.codec
package internal

import scodec.*
import scodec.bits.BitVector

private[codec] object DevFieldCodec:

  def fieldsDecoder(
      definition: DefinitionMessage,
      lookup: DevFieldLookup
  ): Decoder[Vector[DevField]] =
    new Decoder[Vector[DevField]] {
      def decode(bits: BitVector): Attempt[DecodeResult[Vector[DevField]]] =
        val (res, rem) =
          makeFields(bits, definition.devFields, Vector.empty)
        Attempt.successful(DecodeResult(res, rem))

      private def makeDataField(fd: DevFieldDef, fieldData: BitVector): DevField =
        val bo = definition.meta.byteOrder
        lookup.getDevFieldDescription(fd) match
          case Some(fdd) =>
            val bt = fdd.baseType
            val invalid = bt.isInvalid(bo, fieldData)
            DataFieldCodec
              .baseTypeValue(fd.size, bo, bt)
              .complete
              .decodeValue(fieldData) match
              case Attempt.Successful(bv) =>
                TypedDevField(definition.meta, fd, fdd, bv, fieldData, invalid)

              case Attempt.Failure(err) =>
                val reason = UntypedDevField.Reason.Decode(bt, err)
                UntypedDevField(definition.meta, fd, reason, fieldData)

          case None =>
            val reason = UntypedDevField.Reason.NoFieldDescription
            UntypedDevField(definition.meta, fd, reason, fieldData)

      @annotation.tailrec
      final def makeFields(
          data: BitVector,
          fields: List[DevFieldDef],
          current: Vector[DevField]
      ): (Vector[DevField], BitVector) =
        fields match
          case Nil        => (current, data)
          case fd :: tail =>
            val (fieldData, remain) = data.splitAt(fd.size.toBits)
            val f = makeDataField(fd, fieldData)
            makeFields(remain, tail, current.appended(f))
    }

  def fieldEncoder: Encoder[DevField] =
    Encoder {
      case UntypedDevField(_, _, _, data)          => Attempt.successful(data)
      case TypedDevField(meta, _, fdd, _, bits, _) => Attempt.successful(bits)
    }

  def fieldsEncoder: Encoder[Vector[DevField]] =
    Encoder(fieldEncoder.encodeAll)

  def codec(dm: DefinitionMessage, lookup: DevFieldLookup): Codec[Vector[DevField]] =
    Codec(fieldsEncoder, fieldsDecoder(dm, lookup))
