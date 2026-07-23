package fit4s.core

import java.time.Instant
import java.util.UUID

import fit4s.codec.FitBaseType
import fit4s.codec.FitBaseValue
import fit4s.codec.ValueAdjust
import fit4s.core.data.DateTime
import fit4s.profile.MsgField
import fit4s.profile.ProfileEnum

import scodec.bits.ByteVector

trait FieldValueEncoder[A]:
  def fitValue(field: MsgField, a: A): Vector[FitBaseValue]

  def contramap[B](f: B => A): FieldValueEncoder[B] =
    FieldValueEncoder.instance((field, b) => fitValue(field, f(b)))

  def contramap2[B](f: (MsgField, B) => A): FieldValueEncoder[B] =
    FieldValueEncoder.instance((field, b) => fitValue(field, f(field, b)))

object FieldValueEncoder:

  def apply[A](using enc: FieldValueEncoder[A]): FieldValueEncoder[A] = enc

  def instance[A](f: (MsgField, A) => Vector[FitBaseValue]): FieldValueEncoder[A] =
    new FieldValueEncoder[A] {
      def fitValue(field: MsgField, a: A): Vector[FitBaseValue] = f(field, a)
    }

  def single[A](f: (MsgField, A) => FitBaseValue): FieldValueEncoder[A] =
    instance((field, a) => Vector(f(field, a)))

  given [A](using e: FieldValueEncoder[A]): FieldValueEncoder[Option[A]] =
    instance((field, a) => a.map(e.fitValue(field, _)).getOrElse(Vector.empty))

  given forString: FieldValueEncoder[String] =
    single(forFitBaseValue)

  given forByte: FieldValueEncoder[Byte] =
    single(forFitBaseValue)

  given forInt: FieldValueEncoder[Int] =
    single(forFitBaseValue)

  given forShort: FieldValueEncoder[Short] =
    forInt.contramap(_.toInt)

  given forLong: FieldValueEncoder[Long] =
    single(forFitBaseValue)

  given forDouble: FieldValueEncoder[Double] =
    single(forFitBaseValue)

  given forList[T](using e: FieldValueEncoder[T]): FieldValueEncoder[List[T]] =
    instance((field, list) => list.flatMap(e.fitValue(field, _)).toVector)

  given forVector[T](using e: FieldValueEncoder[T]): FieldValueEncoder[Vector[T]] =
    instance((field, list) => list.flatMap(e.fitValue(field, _)))

  given forByteVector: FieldValueEncoder[ByteVector] =
    instance((field, bv) => bv.toIndexedSeq.toVector)

  given forUUID: FieldValueEncoder[UUID] =
    forByteVector.contramap(ByteVector.fromUUID)

  private def forFitBaseValue(field: MsgField, fv: FitBaseValue): FitBaseValue =
    FitBaseType.byName(field.baseTypeName) match
      case Some(fbt) =>
        val adjust = ValueAdjust(field.scale.headOption.getOrElse(1), field.offset)
        adjust.reverse(fbt)(fv)
      case None => fv

  given FieldValueEncoder[ProfileEnum] =
    forInt.contramap(_.ordinal)

  given (using dte: FieldValueEncoder[DateTime]): FieldValueEncoder[Instant] =
    dte.contramap(DateTime.fromInstant)
