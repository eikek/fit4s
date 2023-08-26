package fit4s.cats.syntax

import java.time.Instant

import fit4s.cats.instances
import fit4s.cats.util.{DateInstant, TimeInstant}

object all extends instances.all {

  implicit final class InstantOps(val self: Instant) extends AnyVal {
    def asDate: DateInstant = DateInstant(self)
    def asTime: TimeInstant = TimeInstant(self)
  }
}
