package fit4s

import fit4s.FitMessage.DefinitionMessage
import fit4s.profile.{GenBaseType, Msg}
import scodec.{Attempt, DecodeResult, Decoder, Err}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

object DataDecoder {

  final case class DataDecodeResult(fields: List[FieldDecodeResult])

  sealed trait FieldDecodeResult {
    def widen: FieldDecodeResult = this

    def successValue: Option[Any]
  }
  object FieldDecodeResult {
    final case class UnknownField(localField: FieldDefinition, data: ByteVector)
        extends FieldDecodeResult {
      val successValue = None
    }

    final case class Success(
        localField: FieldDefinition,
        globalField: Msg.Field[_ <: GenBaseType],
        value: Any
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
            println(s"decode field: local=$localField global=n/a")
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
  ): Decoder[FieldDecodeResult] = {
    println(
      s"decode field: local=$localField global=$globalField (fields=${dm.fields.size} vs ${dm.fieldCount})"
    )

    globalField
      .baseTypeCodec(dm.archType)
      .flatMap(value =>
        Decoder.point(FieldDecodeResult.Success(localField, globalField, value))
      )

  }
}
