package fit4s

import munit.FunSuite
import scodec.bits.ByteVector

class SportDecodeTest extends FunSuite with JsonCodec {
  test("decode Sport data") {
    val data = ByteVector.fromValidHex(
      "52616466616872656e000001584a0620000000011d89030000aa000c3d000200010001ff00"
    )
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"LITTLE_ENDIAN","globalMessageNumber":12,"fieldCount":10,"fields":[{"fieldDefNum":3,"sizeBytes":24,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":7},"fitBaseType":7}},{"fieldDefNum":10,"sizeBytes":4,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":4,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":0,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":1,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":5,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":6,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":11,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":12,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":13,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":12}"""
      )
      .fold(throw _, identity)
    val profileMsg = definition.profileMsg.getOrElse(sys.error(s"no profile message"))

    println(profileMsg)
    println(
      DataDecoder
        .create(definition, profileMsg)
        .decode(data.bits)
        .map(_.value)
    )
  }

}
