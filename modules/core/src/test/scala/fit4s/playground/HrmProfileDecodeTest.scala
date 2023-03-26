package fit4s.playground

import fit4s.decode.DataDecoder
import fit4s.FitMessage
import fit4s.json.JsonCodec
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

    println(
      DataDecoder(definition)
        .decode(data.bits)
        .map(_.value)
    )
  }

}
