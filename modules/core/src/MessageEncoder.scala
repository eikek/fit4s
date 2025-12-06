package fit4s.core

import fit4s.codec.FitBaseValue
import fit4s.profile.MsgField
import fit4s.profile.MsgSchema

trait MessageEncoder[A]:
  def encode(a: A): MessageEncoder.EncodedMessage

object MessageEncoder:
  def apply[A](using e: MessageEncoder[A]): MessageEncoder[A] = e

  final case class EncodedField(field: MsgField, fitValue: Vector[FitBaseValue])
  final case class EncodedMessage(msg: MsgSchema, fields: List[EncodedField])

  final class ForMsgApplied[M <: MsgSchema](msg: M):
    def fields[A](enc: (M, A) => List[EncodedField]): MessageEncoder[A] =
      new MessageEncoder[A] {
        def encode(a: A): EncodedMessage =
          EncodedMessage(msg, enc(msg, a))
      }

  def forMsg[M <: MsgSchema, A](msg: M): ForMsgApplied[M] =
    new ForMsgApplied[M](msg)

  object syntax {
    def field[V: FieldValueEncoder](name: MsgField, value: V): EncodedField =
      EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, value))

    extension [V: FieldValueEncoder](self: V)
      def asField(name: MsgField): EncodedField =
        EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, self))
      def ->(name: MsgField): EncodedField =
        EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, self))
    extension (name: MsgField)
      def ->[V: FieldValueEncoder](v: V): EncodedField =
        EncodedField(name, summon[FieldValueEncoder[V]].fitValue(name, v))
  }
