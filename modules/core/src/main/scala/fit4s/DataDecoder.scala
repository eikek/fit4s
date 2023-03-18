package fit4s

import fit4s.FitMessage.DefinitionMessage
import fit4s.profile.{GenBaseType, Msg}
import scodec.{Attempt, DecodeResult, Decoder, Err}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

object DataDecoder {

  final case class DataDecodeResult(fields: List[FieldDecodeResult]) {
    override def toString = {
      val fieldToString =
        fields
          .map {
            case r: FieldDecodeResult.Success =>
              s"${r.globalField.fieldName}=${r.value}"
            case r: FieldDecodeResult.DecodeError =>
              s"Error: ${r.err.messageWithContext}"
            case r: FieldDecodeResult.UnknownField =>
              s"Unknown field: ${r.localField.fieldDefNum}"
          }
          .mkString(", ")

      s"DataDecodeResult($fieldToString)"
    }
  }

  sealed trait FieldDecodeResult {
    def widen: FieldDecodeResult = this

    def successValue: Option[GenBaseType]
  }
  object FieldDecodeResult {
    final case class UnknownField(localField: FieldDefinition, data: ByteVector)
        extends FieldDecodeResult {
      val successValue = None
    }

    final case class Success(
        localField: FieldDefinition,
        globalField: Msg.Field[_ <: GenBaseType],
        value: GenBaseType
    ) extends FieldDecodeResult {
      val successValue = Some(value)
    }

    final case class DecodeError(
        err: Err
    ) extends FieldDecodeResult {
      val successValue = None
    }
  }

  def create(dm: DefinitionMessage, pm: Msg): Decoder[DataDecodeResult] = {
    val fieldDecoders =
      dm.fields.map { localField =>
        pm.findField(localField.fieldDefNum) match {
          case Some(globalField) => decodeField(dm, localField, globalField)
          case None =>
            bytes(localField.sizeBytes).flatMap(bv =>
              Decoder.point(FieldDecodeResult.UnknownField(localField, bv))
            )
        }
      }

    (bits: BitVector) => {
      @annotation.tailrec
      def go(
          decoders: List[Decoder[FieldDecodeResult]],
          input: BitVector,
          results: List[FieldDecodeResult]
      ): Attempt[DecodeResult[List[FieldDecodeResult]]] =
        decoders match {
          case Nil => Attempt.successful(DecodeResult(results, input))
          case d :: m =>
            d.decode(input) match {
              case Attempt.Successful(result) =>
                go(m, result.remainder, result.value :: results)
              case Attempt.Failure(err) =>
                val r = FieldDecodeResult.DecodeError(err)
                go(m, input.drop(8), r :: results)
            }
        }

      go(fieldDecoders, bits, Nil).map(_.map(DataDecodeResult.apply))
    }
  }

  def decodeField(
      dm: DefinitionMessage,
      localField: FieldDefinition,
      globalField: Msg.Field[_ <: GenBaseType]
  ): Decoder[FieldDecodeResult] =
    globalField
      .fieldCodec(dm.archType)
      .flatMap(value =>
        Decoder.point(FieldDecodeResult.Success(localField, globalField, value))
      )
}
