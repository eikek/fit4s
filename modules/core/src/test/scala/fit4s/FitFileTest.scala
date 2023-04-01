package fit4s

import fit4s.profile.messages.FileIdMsg
import munit.CatsEffectSuite

class FitFileTest extends CatsEffectSuite {

  test("read example activity records") {
    for {
      raw <- FitTestData.exampleActivity
      fit = FitFile.decodeUnsafe(raw)
      _ = assertEquals(fit.header, FileHeader(32, 2147, 94080, ".FIT", 17310))
      _ = assertEquals(fit.records.size, 3622)
      _ = assertEquals(fit.dataRecords.size, 3611)
      _ = assertEquals(
        fit.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
        Some(FileIdMsg)
      )
      _ = assertEquals(fit.crc, 15318)
    } yield ()
  }

  test("read some edge 530 activity") {
    for {
      raw <- FitTestData.edge530CyclingActivity
      fit = FitFile.decodeUnsafe(raw)
      _ = assertEquals(fit.header, FileHeader(16, 2172, 95826, ".FIT", 41556))
      _ = assertEquals(fit.records.size, 5689)
      _ = assertEquals(fit.dataRecords.size, 5663)
      _ = assertEquals(
        fit.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
        Some(FileIdMsg)
      )
      _ = assertEquals(fit.crc, 56316)
    } yield ()
  }
}
