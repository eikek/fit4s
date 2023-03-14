package fit4s.profile

import fit4s.profile.basetypes.MesgNum

import java.util.concurrent.atomic.AtomicReference

abstract protected class Msgs {

  private[this] val messages: AtomicReference[Map[MesgNum, Msg]] =
    new AtomicReference[Map[MesgNum, Msg]](Map.empty)

  protected def add[M <: Msg](m: M): M = {
    messages.getAndUpdate(_.updated(m.globalMessageNumber, m))
    m
  }

  def findByMesgNum(n: MesgNum): Option[Msg] =
    messages.get().get(n)

  lazy val allMessages: List[Msg] =
    messages.get().values.toList
}
