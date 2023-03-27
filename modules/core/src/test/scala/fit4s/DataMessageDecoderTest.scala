package fit4s

import fit4s.decode.DataField.KnownField
import fit4s.decode.{DataFields, DataMessageDecoder}
import fit4s.json.JsonCodec
import fit4s.profile.messages.{FileIdMsg, MonitoringMsg, Msg, RecordMsg}
import fit4s.profile.types.{ActivityType, Manufacturer, TypedValue}
import munit.FunSuite
import scodec.bits.ByteVector

class DataMessageDecoderTest extends FunSuite with JsonCodec {

  test("return top-level field if dynamic fields don't match") {

    val data = ByteVector.fromValidHex("04ff000000f8ef593b05fdd856")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":0,"fieldCount":5,"fields":[{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}}],"profileMsg":0,"devFieldCount":0,"devFields":[]}"""
      )
      .fold(throw _, identity)
    val dataMessage = FitMessage.DataMessage(definition, data)

    val fields = DataMessageDecoder.makeDataFields(dataMessage)
    val manufacturerValue = fields
      .getDecodedValue(FileIdMsg.manufacturer)
      .getOrElse(sys.error("Expected manufacturer field"))
      .asInstanceOf[Manufacturer]
    assertEquals(manufacturerValue, Manufacturer.Development)
    val msg = definition.profileMsg.get

    val product =
      fields.get(FileIdMsg.product).getOrElse(sys.error("Expected product field"))
    val expanded = DataMessageDecoder.expandField(msg, fields)(product)
    assertEquals(expanded, DataFields.of(product))
  }

  test("expand dynamic fields") {
    val data = ByteVector.fromValidHex("bc8dd3cb515c753effffffff0100310cffff04")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":0,"fieldCount":7,"fields":[{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":7,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":5,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":0,"devFieldCount":0,"devFields":[]}"""
      )
      .fold(throw _, identity)
    val dataMessage = FitMessage.DataMessage(definition, data)
    val fields = DataMessageDecoder.makeDataFields(dataMessage)
    val msg = definition.profileMsg.get

    val manufacturerValue = fields
      .getDecodedValue(FileIdMsg.manufacturer)
      .getOrElse(sys.error("Expected manufacturer field"))
      .asInstanceOf[Manufacturer]
    assertEquals(manufacturerValue, Manufacturer.Garmin)

    val product =
      fields.get(FileIdMsg.product).getOrElse(sys.error("Expected product field"))
    val expanded = DataMessageDecoder.expandField(msg, fields)(product)
    val expected = KnownField(
      product.local,
      product.byteOrdering,
      FileIdMsg.productGarminProduct.asInstanceOf[Msg.SubField[TypedValue[_]]],
      product.raw
    )
    assertEquals(expanded, DataFields.of(product, expected))
  }

  test("RecordMsg expand components for speed and altitude") {
    val data =
      ByteVector.fromValidHex("8dfb593b34860400e80318a0fa00690a0000000048d9040018")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":20,"fieldCount":9,"fields":[{"fieldDefNum":253,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":5,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":6,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":3,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":4,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":7,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":1,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}}],"profileMsg":20,"devFieldCount":1,"devFields":[{"fieldDefNum":1,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}]}"""
      )
      .fold(throw _, identity)

    val dataMessage = FitMessage.DataMessage(definition, data)
    val fields = DataMessageDecoder.makeDataFields(dataMessage)
    val msg = definition.profileMsg.get

    val speed = fields.get(RecordMsg.speed).getOrElse(sys.error("Expect speed field"))
    val altitude =
      fields.get(RecordMsg.altitude).getOrElse(sys.error("Expect altitude field"))

    val expandedSpeed = DataMessageDecoder.expandField(msg, fields)(speed)
    val expandedAlti = DataMessageDecoder.expandField(msg, fields)(altitude)

    assertEquals(fields.getDecodedValue(RecordMsg.speed).map(_.rawValue), Some(1000L))
    assertEquals(
      expandedSpeed.getDecodedValue(RecordMsg.enhancedSpeed).map(_.rawValue),
      Some(1000L)
    )

    assertEquals(fields.getDecodedValue(RecordMsg.altitude).map(_.rawValue), Some(2665))
    assertEquals(
      expandedAlti.getDecodedValue(RecordMsg.enhancedAltitude).map(_.rawValue),
      Some(2665)
    )
  }

  test("MonitoringMsg expand components for current_activity_type_intensity") {
    val data = ByteVector.fromValidHex("85a468")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"BIG_ENDIAN","globalMessageNumber":55,"fieldCount":2,"fields":[{"fieldDefNum":26,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":24,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":13},"fitBaseType":13}}],"profileMsg":55,"devFieldCount":0,"devFields":[]}"""
      )
      .fold(throw _, identity)
    val dataMessage = FitMessage.DataMessage(definition, data)
    val fields = DataMessageDecoder.makeDataFields(dataMessage)
    val msg = definition.profileMsg.get

    val cati = fields
      .get(MonitoringMsg.currentActivityTypeIntensity)
      .getOrElse(sys.error("Expect current_activity_type_intensity field"))

    val expanded = DataMessageDecoder.expandField(msg, fields)(cati)
    val activityType = expanded.getDecodedValue(MonitoringMsg.activityType)
    val intensity = expanded.getDecodedValue(MonitoringMsg.intensity)

    assertEquals(activityType, Some(ActivityType.Sedentary))
    assertEquals(intensity.map(_.rawValue), Some(3L))
  }
}
