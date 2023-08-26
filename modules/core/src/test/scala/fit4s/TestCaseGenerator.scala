package fit4s

import fit4s.FitMessage.DataMessage
import fit4s.json.JsonCodec
import io.bullet.borer.Json

object TestCaseGenerator extends JsonCodec {
  private val quotes = "\"\"\""

  def makeTestCaseStub(title: String, dm: DataMessage): String =
    s"""
       |test("$title") {
       |  val data = ByteVector.fromValidHex("${dm.raw.toHex}")
       |  val definition = Json.decode($quotes${Json
        .encode(dm.definition)
        .toUtf8String}${quotes}.getBytes)
       |    .to[FitMessage.DefinitionMessage]
       |    .value
       |
       |}
       |""".stripMargin

}
