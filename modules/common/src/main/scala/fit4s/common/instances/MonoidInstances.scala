package fit4s.cats.instances

import cats.Monoid

import fit4s.data._

trait MonoidInstances {
  implicit val distanceMonoid: Monoid[Distance] =
    Monoid.instance(Distance.zero, _ + _)

  implicit val caloriesMonoid: Monoid[Calories] =
    Monoid.instance(Calories.zero, _ + _)

  implicit val temperatureSemigroup: Monoid[Temperature] =
    Monoid.instance(Temperature.zero, _ + _)
}

object MonoidInstances extends MonoidInstances
