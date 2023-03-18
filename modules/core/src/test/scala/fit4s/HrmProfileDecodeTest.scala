package fit4s

import munit.FunSuite
import scodec.bits.ByteVector

class HrmProfileDecodeTest extends FunSuite with JsonCodec {

  test("decode HrmProfile data") {
    val data = ByteVector.fromValidHex("0064")
    val definition = io.circe.parser
      .decode[FitMessage.DefinitionMessage](
        """{"reserved":0,"archType":"BIG_ENDIAN","globalMessageNumber":4,"fieldCount":1,"fields":[{"fieldDefNum":1,"sizeBytes":2,"baseType":{"decoded":{"endianAbility":true,"reserved":0,"baseTypeNum":11},"fitBaseType":139}}],"profileMsg":4}"""
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
