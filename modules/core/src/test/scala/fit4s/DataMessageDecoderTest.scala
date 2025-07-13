package fit4s

import fit4s.data.Distance
import fit4s.data.Speed
import fit4s.decode.DataField.KnownField
import fit4s.decode.{DataFields, DataMessageDecoder}
import fit4s.json.JsonCodec
import fit4s.profile.messages.*
import fit4s.profile.types.{ActivityType, Manufacturer, TypedValue}

import io.bullet.borer.*
import munit.FunSuite
import scodec.bits.ByteVector

class DataMessageDecoderTest extends FunSuite with JsonCodec:

  test("return top-level field if dynamic fields don't match"):

    val data = ByteVector.fromValidHex("04ff000000f8ef593b05fdd856")
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":0,"fieldCount":5,"fields":[{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}}],"profileMsg":0,"devFieldCount":0,"devFields":[]}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value
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

  test("expand dynamic fields"):
    val data = ByteVector.fromValidHex("bc8dd3cb515c753effffffff0100310cffff04")
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":0,"fieldCount":7,"fields":[{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":7,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":5,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":0,"devFieldCount":0,"devFields":[]}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value
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
      FileIdMsg.productGarminProduct.asInstanceOf[Msg.SubField[TypedValue[?]]],
      product.raw
    )
    assertEquals(expanded, DataFields.of(product, expected))

  test("RecordMsg expand components for speed and altitude"):
    val data =
      ByteVector.fromValidHex("8dfb593b34860400e80318a0fa00690a0000000048d9040018")
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":20,"fieldCount":9,"fields":[{"fieldDefNum":253,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":5,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":6,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":3,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":4,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":7,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":1,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}}],"profileMsg":20,"devFieldCount":1,"devFields":[{"fieldDefNum":1,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}]}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value

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

  test("MonitoringMsg expand components for current_activity_type_intensity"):
    val data = ByteVector.fromValidHex("85a468")
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"BIG_ENDIAN","globalMessageNumber":55,"fieldCount":2,"fields":[{"fieldDefNum":26,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":24,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":13},"fitBaseType":13}}],"profileMsg":55,"devFieldCount":0,"devFields":[]}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value
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

  test("decode enhanced speed") {
    val data = ByteVector.fromValidHex(
      "1038d54237e7c32109d63c06de7901002a550000ad18000038130000a900100a91451c0001645e5e9100000000000000000000000000000000000000000000000001000000"
    )
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":20,"fieldCount":18,"fields":[{"fieldDefNum":253,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":0,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":1,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":5,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":29,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":73,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":78,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":7,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":108,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":3,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":4,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":13,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":53,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":107,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":134,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":137,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":138,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":144,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}}],"profileMsg":20,"devFieldCount":5,"devFields":[{"fieldDefNum":0,"sizeBytes":16,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":1,"sizeBytes":8,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":5,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":6,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}]}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value
    val dataMessage = FitMessage.DataMessage(definition, data)
    assertEquals(dataMessage.getField(RecordMsg.speed), Right(None))
    assertEquals(dataMessage.getField(RecordMsg.altitude), Right(None))
    assertEquals(
      dataMessage.getRequiredField(RecordMsg.enhancedSpeed).toOption.flatMap(_.speed),
      Some(Speed.meterPerSecond(6.317))
    )
    assertEquals(
      dataMessage
        .getRequiredField(RecordMsg.enhancedAltitude)
        .toOption
        .flatMap(_.distance),
      Some(Distance.meter(484.0))
    )
  }

  test("read lap") {
    val data = ByteVector.fromValidHex(
      "b25ad542b25ad542510bc5216ec13b0644c4c82136fa330622690c0048290a0020a10700b502000044c4c8216ec13b06510bc52111d13306b26b0100ffffffffffffffffffffffffffffffffffffffffffffffffffffffff541d000018280000ffffffffffffffffffffff7fffffff7fffffff7f00006d00ffff8c00ae0206002900a100ffffffffffffffffffffffff4100420b3a0dffff6300ffff100009018fa5466aff0202ff301a1bff3300ffffffffffff7f7fffffffff1aff000500"
    )
    val definition = Json
      .decode(
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":19,"fieldCount":79,"fields":[{"fieldDefNum":253,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":2,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":5,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":6,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":7,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":8,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":9,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":10,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":27,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":28,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":29,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":30,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":41,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":98,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":102,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":103,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":104,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":105,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":106,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":107,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":110,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":111,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":149,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":8},"fitBaseType":136}},{"fieldDefNum":154,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":8},"fitBaseType":136}},{"fieldDefNum":166,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":167,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":168,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":5},"fitBaseType":133}},{"fieldDefNum":254,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":11,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":12,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":19,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":20,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":21,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":22,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":33,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":34,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":40,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":71,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":96,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":97,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":99,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":121,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":136,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":137,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":143,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":145,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":151,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":155,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":1,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":15,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":16,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":17,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":18,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":23,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":24,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":25,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":26,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":39,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":50,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":51,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":72,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":80,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":81,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":82,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":91,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":92,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":93,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":94,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":95,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":100,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":101,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":108,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":109,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":124,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":152,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":163,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}}],"profileMsg":19,"devFieldCount":1,"devFields":[{"fieldDefNum":4,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}]}""".getBytes
      )
      .to[FitMessage.DefinitionMessage]
      .value
    val dataMessage = FitMessage.DataMessage(definition, data)
    println(dataMessage.getField(LapMsg.enhancedAvgSpeed))
  }
