package fit4s

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import fit4s.profile.messages.FileIdMsg

import munit.CatsEffectSuite

class FitFileTest extends CatsEffectSuite {
  override def munitTimeout = FiniteDuration(3, TimeUnit.MINUTES)

  test("read example activity records") {
    for {
      raw <- FitTestData.exampleActivity
      fits = FitFile.decodeUnsafe(raw)
      _ = assertEquals(fits.toList.size, 1)
      fit = fits.head
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

  // test("read some edge 530 activity") {
  //   for {
  //     raw <- FitTestData.edge530CyclingActivity
  //     fits = FitFile.decodeUnsafe(raw)
  //     _ = assertEquals(fits.toList.size, 1)
  //     fit = fits.head
  //     _ = assertEquals(fit.header, FileHeader(16, 2172, 95826, ".FIT", 41556))
  //     _ = assertEquals(fit.records.size, 5689)
  //     _ = assertEquals(fit.dataRecords.size, 5663)
  //     _ = assertEquals(
  //       fit.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
  //       Some(FileIdMsg)
  //     )
  //     _ = assertEquals(fit.crc, 56316)
  //   } yield ()
  // }

  // test("read some fenix5 file containing multiple fit files concatenated") {
  //   for {
  //     raw <- FitTestData.fenix5Activity
  //     fits = FitFile.decodeUnsafe(raw)
  //     _ = assertEquals(fits.toList.size, 5)
  //     fit1 = fits.head
  //     _ = assertEquals(fit1.header, FileHeader(16, 2078, 18896, ".FIT", 48561))
  //     _ = assertEquals(fit1.records.size, 777)
  //     _ = assertEquals(fit1.dataRecords.size, 757)
  //     _ = assertEquals(
  //       fit1.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
  //       Some(FileIdMsg)
  //     )
  //     _ = assertEquals(fit1.crc, 51554)

  //     fit2 = fits.tail.head
  //     _ = assertEquals(fit2.header, FileHeader(16, 1510, 8176, ".FIT", 6489))
  //     _ = assertEquals(fit2.records.size, 391)
  //     _ = assertEquals(fit2.dataRecords.size, 388)
  //     _ = assertEquals(
  //       fit2.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
  //       Some(HrMsg)
  //     )
  //     _ = assertEquals(fit2.crc, 56218)

  //     fit3 = fits.toList(2)
  //     _ = assertEquals(fit3.header, FileHeader(16, 1510, 8167, ".FIT", 62233))
  //     _ = assertEquals(fit3.records.size, 390)
  //     _ = assertEquals(fit3.dataRecords.size, 387)
  //     _ = assertEquals(
  //       fit3.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
  //       Some(HrMsg)
  //     )
  //     _ = assertEquals(fit3.crc, 36292)

  //     fit4 = fits.toList(3)
  //     _ = assertEquals(fit4.header, FileHeader(16, 1510, 8167, ".FIT", 62233))
  //     _ = assertEquals(fit4.records.size, 390)
  //     _ = assertEquals(fit4.dataRecords.size, 387)
  //     _ = assertEquals(
  //       fit4.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
  //       Some(HrMsg)
  //     )
  //     _ = assertEquals(fit4.crc, 7018)

  //     fit5 = fits.toList(4)
  //     _ = assertEquals(fit5.header, FileHeader(16, 1510, 5479, ".FIT", 37819))
  //     _ = assertEquals(fit5.records.size, 262)
  //     _ = assertEquals(fit5.dataRecords.size, 259)
  //     _ = assertEquals(
  //       fit5.records.head.content.asInstanceOf[FitMessage.DefinitionMessage].profileMsg,
  //       Some(HrMsg)
  //     )
  //     _ = assertEquals(fit5.crc, 58021)
  //   } yield ()
  // }
}
