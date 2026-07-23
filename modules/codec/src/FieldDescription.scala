package fit4s.codec

import fit4s.codec.FitBaseValue$package.FitBaseValue.syntax.*

/** The "profile" of a developer field is the field-description message in the FIT file. */
final case class FieldDescription(
    devDataIdx: Short,
    fieldDefNum: Short,
    baseType: FitBaseType,
    fieldName: Option[String],
    scale: List[Double],
    offset: Double,
    units: List[String],
    bits: List[Int],
    nativeMsgNum: Option[Int]
):
  val devFieldId = DevFieldId(devDataIdx, fieldDefNum)
  def valueAdjust(targetSize: Int) =
    ValueAdjust.from(scale, List(offset), targetSize)

object FieldDescription:
  val globalMesgNum = 206

  def apply(
      devDataIdx: Short,
      fieldDefNum: Short,
      baseType: FitBaseType,
      fieldName: String
  ): FieldDescription =
    FieldDescription(
      devDataIdx,
      fieldDefNum,
      baseType,
      Some(fieldName),
      Nil,
      0,
      Nil,
      Nil,
      None
    )

  /** Decode a data record that is expected to be a field-description. */
  def read(r: DataRecord): Option[FieldDescription] =
    if r.globalMessage != globalMesgNum then None
    else
      for
        devIdx <- Profile.developerDataIndex.firstValue(r).flatMap(_.asUInt)
        fnum <- Profile.fieldDefinitionNumber.firstValue(r).flatMap(_.asUInt)
        bt <- Profile.fitBaseTypeId
          .firstValue(r)
          .flatMap(_.asUInt)
          .map(_.toShort)
          .flatMap(FitBaseType.byFieldByte)
        fn = Profile.fieldName.firstValue(r).flatMap(_.asString)
        off = Profile.offset
          .firstValue(r)
          .flatMap(_.asDouble)
          .getOrElse(0d)
        scale = r
          .fieldData(Profile.scale.fieldDefNum)
          .map(_.flatMap(_.asDouble).toList)
          .getOrElse(Nil)
        units = r
          .fieldData(Profile.units.fieldDefNum)
          .map(_.flatMap(_.asString).flatMap(split).toList)
          .getOrElse(Nil)
        bits = r
          .fieldData(Profile.bits.fieldDefNum)
          .map(_.flatMap(_.asString).flatMap(split).flatMap(_.toIntOption).toList)
          .getOrElse(Nil)
        nmsgNum = Profile.nativeMesgNum.firstValue(r).flatMap(_.asInt)
      yield FieldDescription(
        devIdx.toShort,
        fnum.toShort,
        bt,
        fn,
        scale,
        off,
        units,
        bits,
        nmsgNum
      )

  private def split(s: String): List[String] =
    s.split(',').map(_.trim).filter(_.nonEmpty).toList

  final case class Field(
      fieldDefNum: Int,
      fieldName: String,
      baseType: FitBaseType
  ):
    def firstValue(r: DataRecord): Option[FitBaseValue] =
      r.fieldData(fieldDefNum).flatMap(_.headOption)

  object Profile:
    val developerDataIndex = Field(0, "developer_data_index", FitBaseType.Uint8)
    val fieldDefinitionNumber = Field(1, "field_definition_number", FitBaseType.Uint8)
    val fitBaseTypeId = Field(2, "fit_base_type_id", FitBaseType.Uint8)
    val fieldName = Field(3, "field_name", FitBaseType.string)
    val array = Field(4, "array", FitBaseType.Uint8)
    val components = Field(5, "components", FitBaseType.string)
    val scale = Field(6, "scale", FitBaseType.Uint8)
    val offset = Field(7, "offset", FitBaseType.Sint8)
    val units = Field(8, "units", FitBaseType.string)
    val bits = Field(9, "bits", FitBaseType.string)
    val accumulate = Field(10, "accumulate", FitBaseType.string)
    val fitBaseUnitId = Field(13, "fit_base_unit_id", FitBaseType.Uint16)
    val nativeMesgNum = Field(14, "native_mesg_num", FitBaseType.Uint16)
    val nativeFieldNum = Field(15, "native_field_num", FitBaseType.Uint8)
