package fit4s.common.instances

import cats.Monoid

import fit4s.data._

trait MonoidInstances:
  given Monoid[Distance] =
    Monoid.instance(Distance.zero, _ + _)

  given Monoid[Calories] =
    Monoid.instance(Calories.zero, _ + _)

  given Monoid[Temperature] =
    Monoid.instance(Temperature.zero, _ + _)

object MonoidInstances extends MonoidInstances
