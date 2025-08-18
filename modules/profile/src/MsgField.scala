package fit4s.profile

final case class MsgField(
    fieldDefNum: Int,
    fieldName: String,
    profileType: Option[ProfileType],
    baseTypeName: String,
    components: List[String] = Nil,
    scale: List[Double] = Nil,
    offset: Double = 0,
    units: List[MeasurementUnit] = Nil,
    bits: List[Int] = Nil,
    subFields: List[SubField] = Nil
):
  def merge(sf: SubField): MsgField =
    copy(
      fieldName = sf.fieldName,
      profileType = sf.profileType,
      components = sf.components,
      scale = sf.scale,
      offset = sf.offset,
      units = sf.units,
      bits = sf.bits,
      subFields = Nil
    )

final case class SubField(
    references: List[ReferencedField],
    fieldName: String,
    profileType: Option[ProfileType],
    baseTypeName: String,
    components: List[String] = Nil,
    scale: List[Double] = Nil,
    offset: Double = 0,
    units: List[MeasurementUnit] = Nil,
    bits: List[Int] = Nil
)

final case class ReferencedField(
    refField: MsgField,
    refFieldValue: Int
)
