package fit4s.playground

import fit4s.FitMessage
import fit4s.decode.DataMessageDecoder
import fit4s.json.JsonCodec
import munit.FunSuite
import scodec.bits.ByteVector

class DeviceSettingsDecodeTest extends FunSuite with JsonCodec {
  test("decode DeviceSettings data") {
    val data = ByteVector.fromValidHex(
      "00000000201c000001000000ffffffff64000f0000020100ff00030308143200feffff0101010101000001000000ffff0101000000000000010001000101020202020001010100000101ff03020807020106030400ffffffffffffffffffffffffffffffffffffffffffff010101010101140801010001"
    )
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":2,"fieldCount":67,"fields":[{"fieldDefNum":1,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":2,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":90,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":6},"fitBaseType":134}},{"fieldDefNum":8,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":49,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":58,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":59,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":3,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":4,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":5,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":1},"fitBaseType":1}},{"fieldDefNum":9,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":10,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":11,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":12,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":13,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":14,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":15,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":22,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":26,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":28,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":34,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":35,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":36,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":38,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":41,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":42,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":43,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":44,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":45,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":46,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":48,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":52,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":53,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":54,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":56,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":63,"sizeBytes":11,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":64,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":65,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":66,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":67,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":68,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":69,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":75,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":80,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":81,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":82,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":83,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":84,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":85,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":87,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":94,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":97,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":10},"fitBaseType":10}},{"fieldDefNum":101,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":104,"sizeBytes":30,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":107,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":108,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":109,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":110,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":111,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":112,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":124,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":125,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":126,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":127,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":128,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":133,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":2}"""
      )
      .fold(throw _, identity)
    val profileMsg = definition.profileMsg.getOrElse(sys.error(s"no profile message"))

    println(profileMsg)
    println(
      DataMessageDecoder
        .create(definition)
        .decode(data.bits)
        .map(_.value)
    )
  }

}