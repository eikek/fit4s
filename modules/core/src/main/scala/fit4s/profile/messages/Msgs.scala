package fit4s.profile.messages

import java.util.concurrent.atomic.AtomicReference

import fit4s.profile.types.MesgNum

abstract protected class Msgs:

  private val messages: AtomicReference[Map[MesgNum, Msg]] =
    new AtomicReference[Map[MesgNum, Msg]](Map.empty)

  protected def add[M <: Msg](m: M): M =
    messages.getAndUpdate(_.updated(m.globalMessageNumber, m))
    m

  def findByMesgNum(n: MesgNum): Option[Msg] =
    messages.get().get(n)

  lazy val allMessages: List[Msg] =
    messages.get().values.toList
