package fit4s.codec
package internal

import fit4s.codec.FitBaseValue$package.FitBaseValue.syntax.*
import fit4s.codec.TypedDevField.*

private[codec] object DeveloperProfile:

  def fieldDescription(r: DataRecord): Option[FieldDescription] =
    FieldDescriptionMsg.read(r)

  object DeveloperIdMsg:
    val mesgNum = 207
    val developerId = 0
    val applicationId = 1
    val developerDataIdx = 3

  object FieldDescriptionMsg:
    val mesgNum = 206
    val developerDataIdx = 0 // , FitBaseType.Uint8
    val fieldDefNum = 1
    val fitBaseTypeId = 2 // , FitBaseType.Uint8
    val fieldName = 3 // , FitBaseType.string
    val scale = 6 // , FitBaseType.Uint8
    val offset = 7 // , FitBaseType.Sint8

    def read(r: DataRecord): Option[FieldDescription] =
      if r.globalMessage != mesgNum then None
      else
        for
          devIdx <- r.singleShort(developerDataIdx)
          fdNum <- r.singleShort(fieldDefNum)
          bt <- r.singleShort(fitBaseTypeId).flatMap(FitBaseType.byFieldByte)
          name = r.singleString(fieldName)
          os = r.singleDouble(offset).getOrElse(0d)
          sc = r.doubles(scale).getOrElse(Vector.empty)
        yield FieldDescription(devIdx, fdNum, name, bt, sc.toList, os, r)

  extension (r: DataRecord)
    def singleValue(fieldNum: Int): Option[FitBaseValue] =
      r.fieldData(fieldNum).flatMap(_.headOption)
    def singleInt(fieldNum: Int) =
      r.singleValue(fieldNum)
        .flatMap(_.asByte)
        .map(_ & 0xff)
        .orElse(r.singleValue(fieldNum).flatMap(_.asInt))
    def singleShort(fieldNum: Int) =
      r.singleInt(fieldNum).map(_.toShort)
    def singleDouble(fieldNum: Int): Option[Double] =
      r.singleValue(fieldNum).flatMap(_.asDouble)
    def doubles(fieldNum: Int): Option[Vector[Double]] =
      r.fieldData(fieldNum).map(_.flatMap(_.asDouble))
    def singleString(fieldNum: Int) =
      r.singleValue(fieldNum).map(_.toString)
