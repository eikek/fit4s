package fit4s.common.syntax

import java.time.Instant

import fit4s.common.instances
import fit4s.common.util.{DateInstant, TimeInstant}

object all extends instances.all:

  implicit final class InstantOps(val self: Instant) extends AnyVal:
    def asDate: DateInstant = DateInstant(self)
    def asTime: TimeInstant = TimeInstant(self)
