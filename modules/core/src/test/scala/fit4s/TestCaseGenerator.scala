package fit4s

import fit4s.FitMessage.DataMessage
import fit4s.json.JsonCodec
import io.circe.syntax._

object TestCaseGenerator extends JsonCodec {
  private val quotes = "\"\"\""

  def makeTestCaseStub(title: String, dm: DataMessage): String =
    s"""
       |test("$title") {
       |  val data = ByteVector.fromValidHex("${dm.raw.toHex}")
       |  val definition = io.circe.parser.decode[FitMessage.DefinitionMessage]($quotes${dm.definition.asJson.noSpaces}$quotes).fold(throw _, identity)
       |
       |}
       |""".stripMargin

}
