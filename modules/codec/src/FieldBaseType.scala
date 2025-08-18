package fit4s.codec

final case class FieldBaseType(
    endianAbility: Boolean,
    baseTypeNum: Short
)

object FieldBaseType:

  def from(bt: FitBaseType): FieldBaseType =
    FieldBaseType(
      bt.size.toBytes > 1,
      bt.number
    )
