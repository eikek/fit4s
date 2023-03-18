package fit4s

import munit.FunSuite
import scodec.bits.ByteVector

class UserProfileDecodeTest extends FunSuite with JsonCodec {

  test("decode UserProfile data") {
    val data = ByteVector.fromValidHex("0384011cbe00")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"BIG_ENDIAN","globalMessageNumber":3,"fieldCount":5,"fields":[{"fieldDefNum":4,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":4},"fitBaseType":132}},{"fieldDefNum":1,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}},{"fieldDefNum":2,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":3,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":2},"fitBaseType":2}},{"fieldDefNum":5,"sizeBytes":1,"baseType":{"decoded":{"endianAbility":false,"reserved":0,"baseTypeNum":0},"fitBaseType":0}}],"profileMsg":3}"""
      )
      .fold(throw _, identity)
    val profileMsg = definition.profileMsg.getOrElse(sys.error(s"no profile message"))

    println(
      DataDecoder
        .create(definition, profileMsg)
        .decode(data.bits)
        .map(_.value)
    )
  }

}
