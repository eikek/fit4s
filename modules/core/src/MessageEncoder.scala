package fit4s.core

import fit4s.codec.DeveloperDataId
import fit4s.codec.FieldDescription
import fit4s.codec.FitBaseValue
import fit4s.core.FitMessage.toMsgField
import fit4s.profile.DeveloperDataIdMsg
import fit4s.profile.FieldDescriptionMsg
import fit4s.profile.MsgField
import fit4s.profile.MsgSchema

trait MessageEncoder[A]:
  def encode(a: A): MessageEncoder.EncodedMessage

object MessageEncoder:
  def apply[A](using e: MessageEncoder[A]): MessageEncoder[A] = e

  final case class EncodedField(field: MsgField, fitValue: Vector[FitBaseValue])
  final case class EncodedDevField(
      field: FieldDescription,
      fitValue: Vector[FitBaseValue]
  )
  final case class EncodedMessage(
      msg: MsgSchema,
      fields: List[EncodedField],
      devFields: List[EncodedDevField]
  )

  final class ForMsgApplied[M <: MsgSchema](msg: M):
    def fields[A](enc: (M, A) => List[EncodedField]): MessageEncoder[A] =
      new MessageEncoder[A] {
        def encode(a: A): EncodedMessage =
          EncodedMessage(msg, enc(msg, a), Nil)
      }

    def withDevFields[A](
        enc: (M, A) => (List[EncodedField], List[EncodedDevField])
    ): MessageEncoder[A] =
      new MessageEncoder[A] {
        def encode(a: A): EncodedMessage =
          val (f, d) = enc(msg, a)
          EncodedMessage(msg, f, d)
      }

  def forMsg[M <: MsgSchema, A](msg: M): ForMsgApplied[M] =
    new ForMsgApplied[M](msg)

  object syntax {
    def field[V: FieldValueEncoder](name: MsgField, value: V): EncodedField =
      EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, value))

    extension [V: FieldValueEncoder](self: V)
      def asField(name: MsgField): EncodedField =
        EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, self))

      def asDevField(name: FieldDescription): EncodedDevField =
        EncodedDevField(
          name,
          summon[FieldValueEncoder[V]].fitValue(name.toMsgField, self)
        )

      def ->(name: MsgField): EncodedField =
        EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, self))

    extension (name: MsgField)
      def ->[V: FieldValueEncoder](v: V): EncodedField =
        EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, v))
  }

  given MessageEncoder[FieldDescription] =
    forMsg(FieldDescriptionMsg).fields { (msg, fd) =>
      import syntax.*
      val required = List(
        fd.devDataIdx -> msg.developerDataIndex,
        fd.fieldDefNum -> msg.fieldDefinitionNumber,
        (fd.baseType.fieldByte & 0xff) -> msg.fitBaseTypeId
      )
      val optional: List[Option[EncodedField]] = List(
        fd.fieldName.map(n => msg.fieldName -> n),
        Option.when(fd.scale.nonEmpty)(msg.scale -> fd.scale),
        Option.when(fd.offset != 0)(msg.offset -> fd.offset),
        Option.when(fd.units.nonEmpty)(msg.units -> fd.units),
        Option.when(fd.bits.nonEmpty)(msg.bits -> fd.bits.mkString(","))
      )
      required ++ optional.flatten
    }

  given MessageEncoder[DeveloperDataId] =
    forMsg(DeveloperDataIdMsg).fields { (msg, dd) =>
      import syntax.*
      dd.devIndex -> msg.developerDataIndex
        :: dd.applicationId -> msg.applicationId
        :: dd.appVersion -> msg.applicationVersion
        :: dd.developerId -> msg.developerId
        :: dd.manufacturer -> msg.manufacturerId
        :: Nil
    }
