package fit4s.core

import fit4s.codec.DevFieldDef
import fit4s.codec.DevFieldId
import fit4s.codec.FieldDef
import fit4s.codec.TypedDataField
import fit4s.profile.MsgField

/** Finding a field by providing either the field name or field number. */
trait GetFieldNumber[A]:
  def get(a: A): Int | String

object GetFieldNumber:

  inline def apply[A](using e: GetFieldNumber[A]): GetFieldNumber[A] = e

  private class Impl[A](f: A => Int | String) extends GetFieldNumber[A]:
    def get(a: A) = f(a)
  def instance[A](f: A => Int | String): GetFieldNumber[A] = new Impl(f)

  given GetFieldNumber[Int] = instance(identity)
  given GetFieldNumber[String] = instance(identity)
  given GetFieldNumber[MsgField] = instance(_.fieldDefNum)
  given GetFieldNumber[FieldDef] = instance(_.fieldDefNumber)
  given GetFieldNumber[TypedDataField] = instance(_.fieldDef.fieldDefNumber)
  given GetFieldNumber[DevFieldId] = instance(_.toInt)
  given GetFieldNumber[DevFieldDef] = instance(_.key.toInt)
