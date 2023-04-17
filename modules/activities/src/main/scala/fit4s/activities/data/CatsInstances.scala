package fit4s.activities.data

import cats.{Monoid, Order, Semigroup}

import fit4s.data._

trait CatsInstances {

  implicit val distanceMonoid: Monoid[Distance] =
    Monoid.instance(Distance.zero, _ + _)

  implicit val caloriesMonoid: Monoid[Calories] =
    Monoid.instance(Calories.zero, _ + _)

  implicit val distanceOrder: Order[Distance] =
    Order.fromOrdering

  implicit val caloriesOrder: Order[Calories] =
    Order.fromOrdering

  implicit val heartRateOrder: Order[HeartRate] =
    Order.fromOrdering

  implicit val temperatureSemigroup: Semigroup[Temperature] =
    Semigroup.instance(_ + _)

  implicit val temperatureOrder: Order[Temperature] =
    Order.fromOrdering
}

object CatsInstances extends CatsInstances
