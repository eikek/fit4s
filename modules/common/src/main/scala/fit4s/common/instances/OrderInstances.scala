package fit4s.cats.instances

import cats.Order

import fit4s.data._
import fit4s.profile.types.Sport

trait OrderInstances {
  implicit val distanceOrder: Order[Distance] =
    Order.fromOrdering

  implicit val caloriesOrder: Order[Calories] =
    Order.fromOrdering

  implicit val heartRateOrder: Order[HeartRate] =
    Order.fromOrdering

  implicit val temperatureOrder: Order[Temperature] =
    Order.fromOrdering

  implicit val sportOrder: Order[Sport] =
    Order.by(_.rawValue)
}

object OrderInstances extends OrderInstances
