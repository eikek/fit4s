package fit4s.core

import fit4s.profile.MsgSchema

trait GetMesgNum[A]:
  def get(a: A): Int

object GetMesgNum:

  inline def apply[A](using e: GetMesgNum[A]): GetMesgNum[A] = e

  private class Impl[A](f: A => Int) extends GetMesgNum[A]:
    def get(a: A) = f(a)
  def instance[A](f: A => Int): GetMesgNum[A] = new Impl(f)

  given GetMesgNum[Int] = instance(identity)
  given [S <: MsgSchema]: GetMesgNum[S] = instance(_.globalNum)
