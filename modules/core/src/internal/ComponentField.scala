package fit4s.core.internal

import scala.collection.mutable.ListBuffer

import fit4s.profile.MeasurementUnit
import fit4s.profile.MsgField

final private[core] case class ComponentField(
    name: String,
    unit: Option[MeasurementUnit],
    scale: Double,
    offset: Double,
    bits: Int
):

  def merge(targetField: MsgField): MsgField =
    targetField.copy(
      units = unit.map(List(_)).getOrElse(targetField.units),
      scale = if scale != 0 then List(scale) else targetField.scale,
      offset = if offset != 1 then offset else targetField.offset
    )

private[core] object ComponentField:
  def from(parentSchema: MsgField): List[ComponentField] = {
    val compCount = parentSchema.components.size
    val units = parentSchema.units.fillTo(compCount, Nil)
    val scales = parentSchema.scale.fillTo(compCount, List(1.0))
    val offsets = List(parentSchema.offset).fillTo(compCount, List(0.0))
    val bits = parentSchema.bits.fillTo(compCount, Nil)
    val buffer = ListBuffer.empty[ComponentField]
    for (i <- 0 until compCount)
      buffer.addOne(
        ComponentField(
          parentSchema.components(i),
          units.lift(i),
          scales(i),
          offsets(i),
          bits(i)
        )
      )
    buffer.toList
  }

  extension [A](self: List[A])
    def fillTo(n: Int, default: List[A]): List[A] =
      if self.isEmpty && default.isEmpty then Nil
      else if self.isEmpty then default.fillTo(n, Nil)
      else LazyList.continually(self).flatten.take(n).toList
