package fit4s

import cats.effect.IO

import fit4s.profile.messages.HrMsg

import munit.CatsEffectSuite

class HrMessageDecodeTest extends CatsEffectSuite {

  // TODO implement decoding this data
  test("hr messages matching records") {
    for {
      raw <- FitTestData.fenix5Activity
      fits = FitFile.decodeUnsafe(raw)
      _ = assertEquals(fits.toList.size, 5)
      // HrMsg are in concatenated fit files that don't have a fileId
      // these messages contain hr data that must be matched to a corresponding
      // previous record message by the timestamp
      hrMsgs = fits.tail.flatMap(_.dataRecords.filter(_.isMessage(HrMsg)))

      first = hrMsgs.head
      _ <- IO.println(first.getField(HrMsg.timestamp))
      _ <- IO.println(first.getField(HrMsg.eventTimestamp))
      _ <- IO.println(first.getField(HrMsg.fractionalTimestamp))
      _ <- IO.println(first.getField(HrMsg.filteredBpm))

      _ <- IO.println("----------------")
      second = hrMsgs(1)
      _ <- IO.println(second.getField(HrMsg.eventTimestamp))
      _ <- IO.println(second.getField(HrMsg.eventTimestamp12))
      _ <- IO.println(second.getField(HrMsg.filteredBpm))
    } yield ()
  }
}
