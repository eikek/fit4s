package fit4s.common.instances

import cats.Order

import fit4s.profile.types.Sport

import _root_.fit4s.data.*

trait OrderInstances:
  given Order[Distance] = Order.fromOrdering

  given Order[Calories] = Order.fromOrdering

  given Order[HeartRate] = Order.fromOrdering

  given Order[Temperature] = Order.fromOrdering

  given Order[Sport] = Order.by(_.rawValue)

object OrderInstances extends OrderInstances
