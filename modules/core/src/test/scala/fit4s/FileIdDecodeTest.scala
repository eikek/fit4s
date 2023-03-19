package fit4s

import fit4s.profile.messages.FileIdMsg
import munit.FunSuite
import scodec.bits.ByteVector
//import scodec.codecs._

//@annotation.nowarn
class FileIdDecodeTest extends FunSuite with JsonCodec {

  test("decode FileId data (2)") {
    val data = ByteVector.fromValidHex("220a24eb63681234ffffffff0100890affff04")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":0,"fieldCount":7,"fields":[{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":4,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":7,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":5,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":0}"""
      )
      .fold(throw _, identity)
    val profileMsg: FileIdMsg.type = definition.profileMsg
      .getOrElse(sys.error(s"no profile message"))
      .asInstanceOf[FileIdMsg.type]

    val decodeSerialNum =
      profileMsg.serialNumber.fieldCodec(definition.fields.head)(definition.archType)
    println(decodeSerialNum.decode(data.take(4).bits))

    val decodeCreated =
      profileMsg.timeCreated.fieldCodec(definition.fields(1))(definition.archType)
    println(decodeCreated.decode(data.drop(4).take(4).bits))

    println(
      DataDecoder(definition)
        .decode(data.bits)
        .map(_.value)
    )
  }

  test("decode FileId data") {
    val data = ByteVector.fromValidHex("000103dc0001e24002")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"BIG_ENDIAN","globalMessageNumber":0,"fieldCount":4,"fields":[{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":2,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":3,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":12},"fitBaseType":140}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":0}"""
      )
      .fold(throw _, identity)

//    val decodeManufacturer = profileMsg.manufacturer.fieldCodec(definition.archType)
//    println(decodeManufacturer.decode(data.take(2).bits))
//
//    val decodeProduct = profileMsg.product.fieldCodec(definition.archType)
//    println(decodeProduct.decode(data.drop(2).take(2).bits))
//
//    val decodeSerialNum = profileMsg.serialNumber.fieldCodec(definition.archType)
//    println(decodeSerialNum.decode(data.drop(4).take(4).bits))

    println(
      DataDecoder(definition)
        .decode(data.bits)
        .map(_.value)
    )
  }

}
