package fit4s

import fit4s.FitMessage.DefinitionMessage
import fit4s.profile.basetypes.{FitBaseType, LongBaseType, StringBaseType}
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
              s"${r.globalField.fieldName}=${r.valueString}"
            case r: FieldDecodeResult.DecodeError =>
              s"Error: ${r.err.messageWithContext}"
            case r: FieldDecodeResult.UnknownField =>
              s"Unknown field: ${r.localField.fieldDefNum}"
            case r: FieldDecodeResult.NoReferenceSubfield =>
              s"No subfield reference: ${r.globalField.fieldName}"
          }
          .mkString(", ")

      s"DataDecodeResult($fieldToString)"
    }
  }

  sealed trait FieldDecodeResult {
    def widen: FieldDecodeResult = this

    def messageField: Option[Msg.Field[GenBaseType]]
    def successValue: Option[GenBaseType]
  }
  object FieldDecodeResult {
    final case class UnknownField(localField: FieldDefinition, data: ByteVector)
        extends FieldDecodeResult {
      val successValue = None
      val messageField = None
    }

    final case class Success(
        localField: FieldDefinition,
        globalField: Msg.Field[GenBaseType],
        value: GenBaseType
    ) extends FieldDecodeResult {
      val successValue = Some(value)
      val messageField = Some(globalField)

      // TODO support for arrays
      def scaledValue: Option[Double] =
        (value, globalField.scale) match {
          case (LongBaseType(rv, _), List(scale)) =>
            Some(rv / scale)
          case _ => None
        }

      def valueString: String = {
        val amount = scaledValue
          .map(_.toString)
          .getOrElse(value match {
            case LongBaseType(rv, _) => rv.toString
            case _                   => value.toString
          })
        val unit = globalField.unit.map(_.name).getOrElse("")
        s"$amount$unit"
      }
    }

    final case class DecodeError(
        err: Err
    ) extends FieldDecodeResult {
      val successValue = None
      val messageField = None
    }

    final case class NoReferenceSubfield(
        localField: FieldDefinition,
        globalField: Msg.Field[GenBaseType]
    ) extends FieldDecodeResult {
      val successValue = None
      val messageField = Some(globalField)
    }
  }

  def create(dm: DefinitionMessage, pm: Msg): Decoder[DataDecodeResult] = {
    def fieldDecoder(previous: List[FieldDecodeResult], localField: FieldDefinition) =
      pm.findField(localField.fieldDefNum) match {
        case Some(globalField) =>
          decodeField(previous, dm, localField, globalField)

        case None =>
          bytes(localField.sizeBytes).flatMap(bv =>
            Decoder.point(FieldDecodeResult.UnknownField(localField, bv))
          )
      }

    (bits: BitVector) => {
      @annotation.tailrec
      def go(
          fields: List[FieldDefinition],
          input: BitVector,
          results: List[FieldDecodeResult]
      ): Attempt[DecodeResult[List[FieldDecodeResult]]] =
        fields match {
          case Nil => Attempt.successful(DecodeResult(results, input))
          case field :: m =>
            fieldDecoder(results, field).decode(input) match {
              case Attempt.Successful(result) =>
                go(m, result.remainder, result.value :: results)
              case Attempt.Failure(err) =>
                val r = FieldDecodeResult.DecodeError(err)
                go(m, input.drop(8), r :: results)
            }
        }

      go(dm.fields, bits, Nil).map(_.map(DataDecodeResult.apply))
    }
  }

  def decodeField(
      previous: List[FieldDecodeResult],
      dm: DefinitionMessage,
      localField: FieldDefinition,
      globalField: Msg.Field[GenBaseType]
  ): Decoder[FieldDecodeResult] =
    if (globalField.isDynamicField) {
      // look in previous decoded values, if the referenced field matches
      val subFieldMatch =
        globalField.subFields.find { subField =>
          previous.collectFirst { case p: FieldDecodeResult.Success =>
            subField.references.exists(ref =>
              ref.refField == p.globalField && ref.refFieldValue == p.value
            )
          }.isDefined
        }

      subFieldMatch match {
        case Some(subField) =>
          subField
            .fieldCodec(dm.archType)
            .flatMap(value =>
              Decoder.point(FieldDecodeResult.Success(localField, globalField, value))
            )
        case None =>
          Decoder.point(FieldDecodeResult.NoReferenceSubfield(localField, globalField))
      }

    } else {
      if (globalField.fieldBaseType == FitBaseType.String) {
        StringBaseType
          .codec(localField.sizeBytes)
          .asDecoder
          .map(value => FieldDecodeResult.Success(localField, globalField, value))

      } else {

        globalField
          .fieldCodec(dm.archType)
          .flatMap(value =>
            Decoder.point(FieldDecodeResult.Success(localField, globalField, value))
          )
      }
    }
}
